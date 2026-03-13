package az.fitnest.identity.service.impl;

import az.fitnest.identity.client.UserSubscriptionGrpcClient;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.util.MobileNumberUtils;
import az.fitnest.identity.dto.UpdateUserProfileCommand;
import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.model.entity.AuthToken;
import az.fitnest.identity.model.entity.Role;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.exception.ResourceNotFoundException;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final java.util.regex.Pattern NAME_PART_PATTERN = java.util.regex.Pattern.compile("\\S+");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenRepository authTokenRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTokenService redisTokenService;
    private final IdentityEventPublisher eventPublisher;
    private final ApplicationEventPublisher localEventPublisher;
    private final PasswordService passwordService;
    private final OtpService otpService;
    private final UserSubscriptionGrpcClient userSubscriptionGrpcClient;

    private Role defaultUserRole;

    @PostConstruct
    public void initDefaultRole() {
        this.defaultUserRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new IllegalStateException("ROLE_USER missing"));
    }

    @Transactional
    @Override
    public User updateUserRole(Long userId, String roleName) {
        User user = getUserById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found", "RESOURCE_NOT_FOUND"));

        user.setRole(role);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public User getUserById(Long userId) {
        return getUserOrThrow(userId);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    @Override
    public User createNewUser(String firstName, String lastName, String passwordHash, String mobile) {
        String normalizedMobile = MobileNumberUtils.normalize(mobile);
        if (normalizedMobile == null || normalizedMobile.isBlank()) {
            throw new az.fitnest.identity.exception.ValidationException("error.validation", "INVALID_MOBILE");
        }
        return createNewUserInternal(normalizeNamePart(firstName), normalizeNamePart(lastName), passwordHash, normalizedMobile);
    }

    @Override
    public User createNewUserWithFullName(String fullName, String passwordHash, String mobile) {
        String normalizedMobile = MobileNumberUtils.normalize(mobile);
        if (normalizedMobile == null || normalizedMobile.isBlank()) {
            throw new az.fitnest.identity.exception.ValidationException("error.validation", "INVALID_MOBILE");
        }
        NameParts parts = splitFullName(fullName);
        return createNewUserInternal(parts.firstName(), parts.lastName(), passwordHash, normalizedMobile);
    }

    private User createNewUserInternal(String firstName, String lastName, String passwordHash, String mobile) {
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .passwordHash(passwordHash)
                .mobile(mobile)
                .hasAccount(true)
                .setupRequired(true)
                .failedLoginAttempts(0)
                .status(UserStatus.ACTIVE)
                .role(defaultUserRole)
                .build();
        try {
            return userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException("error.service.operation_not_allowed", "DUPLICATE_MOBILE");
        }
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public User updateUserProfile(Long userId, UpdateUserProfileCommand command) {
        User user = getUserOrThrow(userId);
        String firstName = command.firstName();
        String lastName = command.lastName();
        boolean namePartsProvided = firstName != null || lastName != null;
        if (namePartsProvided) {
            NameParts parts = resolveNameParts(firstName, lastName, null);
            user.setFirstName(parts.firstName());
            user.setLastName(parts.lastName());
        }
        User saved = userRepository.save(user);
        localEventPublisher.publishEvent(new UserUpdatedEvent(userId));
        return saved;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserUpdated(UserUpdatedEvent event) {
        publishUserEvent("USER_UPDATED", event.userId());
    }

    @Override
    @Transactional
    public az.fitnest.identity.dto.OtpSendResponse requestEmailChange(Long userId, String newEmail) {
        User user = getUserOrThrow(userId);
        boolean canChange = !newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.findFirstByEmail(newEmail.toLowerCase()).isEmpty();
        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.EMAIL_CHANGE, null, newEmail);
        if (canChange) {
            return otpService.sendOtpByUserId(userId, otpRequest);
        } else {
            return new az.fitnest.identity.dto.OtpSendResponse(null, null, null, "success.otp.sent_if_exists");
        }
    }

    @Override
    @Transactional
    public User confirmEmailChange(Long userId, String otpSessionId, String otpCode) {
        User user = getUserOrThrow(userId);
        var verificationResult = otpService.verifyOtp(otpSessionId, otpCode);
        if (verificationResult.purpose() != OtpPurpose.EMAIL_CHANGE) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException("error.service.invalid_operation_context");
        }
        String newEmail = verificationResult.email();
        user.setEmail(newEmail.trim().toLowerCase());
        User saved = userRepository.save(user);
        localEventPublisher.publishEvent(new UserUpdatedEvent(userId));
        return saved;
    }

    @Override
    @Transactional
    public az.fitnest.identity.dto.OtpSendResponse requestMobileChange(Long userId, String newMobile) {
        User user = getUserOrThrow(userId);
        String normalizedMobile = MobileNumberUtils.normalize(newMobile);
        boolean canChange = !normalizedMobile.equals(user.getMobile()) && userRepository.findFirstByMobile(normalizedMobile).isEmpty();
        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.MOBILE_CHANGE, normalizedMobile, null);
        if (canChange) {
            return otpService.sendOtpByUserId(userId, otpRequest);
        } else {
            return new az.fitnest.identity.dto.OtpSendResponse(null, null, null, "success.otp.sent_if_exists");
        }
    }

    @Override
    @Transactional
    public User confirmMobileChange(Long userId, String otpSessionId, String otpCode) {
        User user = getUserOrThrow(userId);
        var verificationResult = otpService.verifyOtp(otpSessionId, otpCode);

        if (verificationResult.purpose() != OtpPurpose.MOBILE_CHANGE) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException("error.service.invalid_operation_context");
        }

        String normalizedMobile = verificationResult.mobile();

        user.setMobile(normalizedMobile);
        User saved = userRepository.save(user);
        publishUserEvent("USER_UPDATED", userId);
        return saved;
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public User updateProfileImageUrl(Long userId, String profileImageUrl) {
        User user = getUserOrThrow(userId);

        user.setProfileImageUrl(profileImageUrl);
        User saved = userRepository.save(user);
        publishUserEvent("USER_UPDATED", userId);
        return saved;
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public User updateSetupRequired(Long userId, boolean setupRequired) {
        User user = getUserById(userId);
        user.setSetupRequired(setupRequired);
        User saved = userRepository.save(user);
        if (!setupRequired) {
            localEventPublisher.publishEvent(new UserSetupCompletedEventLocal(userId));
        }
        return saved;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserSetupCompleted(UserSetupCompletedEventLocal event) {
        try {
            eventPublisher.publishSetupCompleted(event.userId());
        } catch (Exception e) {
            log.error("Failed to publish setup completed event for userId {}", event.userId(), e);
        }
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public User updateLanguage(Long userId, String language) {
        User user = getUserOrThrow(userId);
        user.setLanguage(language);
        User saved = userRepository.save(user);
        return saved;
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public void deactivateAccount(Long userId, az.fitnest.identity.dto.DeactivateAccountRequest request) {
        String reason = (request != null) ? request.reason() : null;
        deactivateUser(userId, reason != null && !reason.isBlank() ? reason : "Self-deactivation");
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public void deactivateUser(Long userId, String reason) {
        User user = getUserOrThrow(userId);

        user.setStatus(UserStatus.INACTIVE);
        user.setSessionStatus(az.fitnest.identity.model.enums.SessionStatus.NO_SESSIONS);
        user.setInactiveAt(java.time.Instant.now());
        userRepository.save(user);

        redisTokenService.removeAllSessions(userId);

        authTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    @Override
    public void deactivateAllUsers() {
        userRepository.deactivateAllNonAdmins(java.time.Instant.now());
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteInactiveAccountsAfter30Days() {
        Instant threshold = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        int batchSize = 1000;
        userRepository.deleteInactiveUsersBeforeBatch(threshold, batchSize);
    }

    @Transactional
    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmNewPassword) {
        User user = getUserById(userId);
        if (!passwordService.verifyPassword(oldPassword, user.getPasswordHash()).matches()) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException("error.auth.invalid_credentials");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            throw new az.fitnest.identity.exception.ValidationException("error.validation", "VALIDATION_ERROR");
        }
        if (!passwordService.isStrongPassword(newPassword)) {
            throw new az.fitnest.identity.exception.ValidationException("error.validation", "WEAK_PASSWORD");
        }
        if (passwordService.isPasswordReused(userId, newPassword)) {
            throw new az.fitnest.identity.exception.ValidationException("error.validation", "PASSWORD_REUSED");
        }
        user.setPasswordHash(passwordService.hashPassword(newPassword));
        userRepository.save(user);
        localEventPublisher.publishEvent(new PasswordChangedEvent(userId));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordChanged(PasswordChangedEvent event) {
        redisTokenService.removeAllSessions(event.userId());
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public void deleteAccount(Long userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(UserStatus.INACTIVE);
        user.setInactiveAt(java.time.Instant.now());
        userRepository.save(user);
        publishUserEvent("ACCOUNT_DEACTIVATED", userId);
        localEventPublisher.publishEvent(new UserAccountDeletedEventLocal(userId));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserAccountDeleted(UserAccountDeletedEventLocal event) {
        try {
            redisTokenService.removeAllSessions(event.userId());
            authTokenRepository.deleteByUserId(event.userId());
        } catch (Exception e) {
            log.error("Failed to cleanup after account deletion for userId {}", event.userId(), e);
        }
    }

    @Transactional
    @Override
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found", "RESOURCE_NOT_FOUND"));
        if (userRepository.existsByRole(role)) {
            throw new ConflictException("error.role.in_use", "ROLE_IN_USE");
        }
        roleRepository.deleteById(roleId);
    }

    @Transactional
    @Override
    public User updateSessionStatus(Long userId, az.fitnest.identity.model.enums.SessionStatus sessionStatus) {
        User user = getUserOrThrow(userId);
        user.setSessionStatus(sessionStatus);
        return userRepository.save(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found", "RESOURCE_NOT_FOUND"));
    }

    private NameParts resolveNameParts(String firstName, String lastName, String fullName) {
        String fn = normalizeNamePart(firstName);
        String ln = normalizeNamePart(lastName);
        if (fn != null || ln != null) {
            return new NameParts(fn, ln);
        }
        return splitFullName(fullName);
    }

    private NameParts splitFullName(String fullName) {
        if (fullName == null) {
            return new NameParts(null, null);
        }
        String v = fullName.trim();
        if (v.isEmpty()) {
            return new NameParts(null, null);
        }
        String[] parts = v.split("\\s+");
        if (parts.length == 1) {
            return new NameParts(parts[0], null);
        }
        String first = parts[0];
        String last = String.join(" ", java.util.Arrays.asList(parts).subList(1, parts.length));
        return new NameParts(first, last);
    }

    private String normalizeNamePart(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) return null;
        java.util.regex.Matcher matcher = NAME_PART_PATTERN.matcher(v);
        return matcher.find() ? matcher.group() : null;
    }

    private void publishUserEvent(String eventType, Long userId) {
        UserEvent event = new UserEvent(eventType, userId, System.currentTimeMillis());
        kafkaTemplate.send("user-events", userId.toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish user event", ex);
                }
            });
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> getAllUsersMapped(int page, int size) {
        size = Math.min(size, 100);
        return userRepository.findAll(PageRequest.of(Math.max(0, page - 1), size))
                .map(UserResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> searchUsers(int page, int size, Long id, String name, String surname, String email, String mobile) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);
        return userRepository.searchUsers(id, name, surname, email, mobile, pageable)
                .map(UserResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> searchUsersAdvanced(int page, int size, String query, Long packageID, Integer durationMonths) {
        size = Math.min(size, 100);
        Long id = null;
        String name = null;
        String surname = null;
        String email = null;
        String mobile = null;
        String genericSearch = null;
        if (query != null && !query.isBlank()) {
            if (!query.contains("=")) {
                genericSearch = query.trim();
            } else {
                String[] parts = query.split(";");
                for (String part : parts) {
                    String[] kv = part.split("=", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().toLowerCase();
                        String value = kv[1].trim();
                        switch (key) {
                            case "id":
                                try { id = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                                break;
                            case "name":
                                name = value;
                                break;
                            case "surname":
                                surname = value;
                                break;
                            case "email":
                                email = value;
                                break;
                            case "mobile":
                                mobile = value;
                                break;
                        }
                    }
                }
            }
        }
        if (genericSearch != null) {
            name = genericSearch;
            surname = genericSearch;
            email = genericSearch;
            mobile = genericSearch;
        }
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);
        Page<User> userPage = userRepository.searchUsersAdvanced(id, name, surname, email, mobile, pageable);
        List<User> filteredUsers = userPage.getContent();
        if (packageID != null) {
            try {
                List<Long> packageUserIds = userSubscriptionGrpcClient.getUserIdsByPackageId(packageID);
                filteredUsers = filteredUsers.stream()
                    .filter(user -> packageUserIds.contains(user.getId()))
                    .toList();
            } catch (Exception e) {
                log.error("Failed to fetch user IDs by package ID {} via gRPC. Skipping package filter.", packageID, e);
            }
        }
        if (durationMonths != null) {
            try {
                List<Long> durationUserIds = userSubscriptionGrpcClient.getUserIdsByDurationMonths(durationMonths);
                filteredUsers = filteredUsers.stream()
                    .filter(user -> durationUserIds.contains(user.getId()))
                    .toList();
            } catch (UnsupportedOperationException e) {
                log.warn("Duration months filtering not implemented in gRPC client.");
            } catch (Exception e) {
                log.error("Failed to fetch user IDs by duration months {} via gRPC. Skipping duration filter.", durationMonths, e);
            }
        }
        int start = Math.min(page * size, filteredUsers.size());
        int end = Math.min(start + size, filteredUsers.size());
        List<UserResponse> pagedResponses = filteredUsers.subList(start, end).stream()
            .map(UserResponseMapper::toResponse)
            .toList();
        return new org.springframework.data.domain.PageImpl<>(pagedResponses, pageable, filteredUsers.size());
    }

    @Transactional(readOnly = true)
    @Override
    public Page<az.fitnest.identity.dto.AdminUserResponse> getAdminUsers(int page, int size, String query, Long packageID, Integer durationMonths) {
        size = Math.min(size, 100);
        Long id = null;
        String name = null;
        String surname = null;
        String email = null;
        String mobile = null;
        String genericSearch = null;
        if (query != null && !query.isBlank()) {
            if (!query.contains("=")) {
                genericSearch = query.trim();
            } else {
                String[] parts = query.split(";");
                for (String part : parts) {
                    String[] kv = part.split("=", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().toLowerCase();
                        String value = kv[1].trim();
                        switch (key) {
                            case "id":
                                try { id = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                                break;
                            case "name":
                                name = value;
                                break;
                            case "surname":
                                surname = value;
                                break;
                            case "email":
                                email = value;
                                break;
                            case "mobile":
                                mobile = value;
                                break;
                        }
                    }
                }
            }
        }
        if (genericSearch != null) {
            name = genericSearch;
            surname = genericSearch;
            email = genericSearch;
            mobile = genericSearch;
        }
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);
        Page<User> userPage = userRepository.searchUsersAdvanced(id, name, surname, email, mobile, pageable);
        List<User> filteredUsers = userPage.getContent();
        if (packageID != null) {
            try {
                List<Long> packageUserIds = userSubscriptionGrpcClient.getUserIdsByPackageId(packageID);
                filteredUsers = filteredUsers.stream()
                    .filter(user -> packageUserIds.contains(user.getId()))
                    .toList();
            } catch (Exception e) {
                log.error("Failed to fetch user IDs by package ID {} via gRPC. Skipping package filter.", packageID, e);
            }
        }
        if (durationMonths != null) {
            try {
                List<Long> durationUserIds = userSubscriptionGrpcClient.getUserIdsByDurationMonths(durationMonths);
                filteredUsers = filteredUsers.stream()
                    .filter(user -> durationUserIds.contains(user.getId()))
                    .toList();
            } catch (UnsupportedOperationException e) {
                log.warn("Duration months filtering not implemented in gRPC client.");
            } catch (Exception e) {
                log.error("Failed to fetch user IDs by duration months {} via gRPC. Skipping duration filter.", durationMonths, e);
            }
        }
        List<az.fitnest.identity.dto.AdminUserResponse> adminResponses = filteredUsers.stream()
            .map(user -> {
                az.fitnest.order.grpc.ActiveSubscriptionResponse sub = null;
                try {
                    sub = userSubscriptionGrpcClient.getActiveSubscription(user.getId());
                } catch (Exception e) {
                    log.warn("Failed to fetch subscription info for user {}", user.getId(), e);
                }
                String subscriptionStatus = (sub != null && sub.getSubscriptionStatus() != null && !sub.getSubscriptionStatus().isEmpty()) ? sub.getSubscriptionStatus() : null;
                return new az.fitnest.identity.dto.AdminUserResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getMobile(),
                    user.getEmail(),
                    user.getStatus() != null ? user.getStatus().name() : null,
                    subscriptionStatus
                );
            })
            .toList();
        return new org.springframework.data.domain.PageImpl<>(adminResponses, pageable, userPage.getTotalElements());
    }

    private record UserEvent(String eventType, Long userId, long timestamp) {}
    private record UserUpdatedEvent(Long userId) {}
    private record PasswordChangedEvent(Long userId) {}
    private record UserAccountDeletedEventLocal(Long userId) {
    }

    private record UserSetupCompletedEventLocal(Long userId) {
    }

    private record NameParts(String firstName, String lastName) {
    }
}
