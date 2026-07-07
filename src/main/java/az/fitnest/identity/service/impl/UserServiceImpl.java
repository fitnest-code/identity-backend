package az.fitnest.identity.service.impl;

import az.fitnest.identity.client.UserSubscriptionGrpcClient;
import az.fitnest.identity.dto.request.OtpSendRequest;
import az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest;
import az.fitnest.identity.dto.response.UserResponse;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.exception.ResourceNotFoundException;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.model.entity.Role;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.UserDeviceRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.util.MobileNumberUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final java.util.regex.Pattern NAME_PART_PATTERN = java.util.regex.Pattern.compile("^[\\p{L}\\s\\-\\.\\'\\(\\)\\d]+$");

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
    private final UserResponseMapper userResponseMapper;
    private final az.fitnest.identity.service.UserProfileGrpcClient userProfileGrpcClient;
    private final UserDeviceRepository userDeviceRepository;
    private final TestUserHelper testUserHelper;

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public User updateUserRole(Long userId, String roleName) {
        User user = getUserById(userId);
        String finalRoleName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        Role role = roleRepository.findByName(finalRoleName)
                .orElseThrow(() -> new az.fitnest.identity.exception.BadRequestException("error.resource.not_found"));
        user.setRole(role);
        User saved = userRepository.save(user);
        publishUserEvent("USER_UPDATED", userId);
        log.info("User {} role changed to {}", userId, finalRoleName);
        return saved;
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
        return createNewUserInternal(normalizeNamePart(firstName), normalizeNamePart(lastName), passwordHash,
                normalizedMobile);
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
                .passwordHash(passwordHash)
                .mobile(mobile)
                .hasAccount(true)
                .setupRequired(true)
                .failedLoginAttempts(0)
                .status(UserStatus.ACTIVE)
                .hasLocalPassword(true)
                .role(getDefaultUserRole())
                .build();
        try {
            User saved = userRepository.save(user);
            log.info("Creating user profile in user-backend for user ID: {}", saved.getId());
            userProfileGrpcClient.createUserProfile(saved.getId(), firstName, lastName, null); // Email is null here because createNewUser doesn't take email.
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException("error.service.operation_not_allowed", "DUPLICATE_MOBILE");
        }
    }

    @Override
    public User createNewUserV3(String firstName, String lastName, String mobile) {
        String normalizedMobile = MobileNumberUtils.normalize(mobile);
        if (normalizedMobile == null || normalizedMobile.isBlank()) {
            throw new az.fitnest.identity.exception.ValidationException("error.validation", "INVALID_MOBILE");
        }
        User user = User.builder()
                .passwordHash(null)
                .mobile(normalizedMobile)
                .hasAccount(true)
                .setupRequired(true)
                .failedLoginAttempts(0)
                .status(UserStatus.ACTIVE)
                .hasLocalPassword(false)
                .role(getDefaultUserRole())
                .build();
        try {
            User saved = userRepository.save(user);
            log.info("Creating user profile (V3 passwordless) in user-backend for user ID: {}", saved.getId());
            userProfileGrpcClient.createUserProfile(saved.getId(), normalizeNamePart(firstName), normalizeNamePart(lastName), null);
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException("error.service.operation_not_allowed", "DUPLICATE_MOBILE");
        }
    }

    private Role getDefaultUserRole() {
        return roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    return roleRepository.save(role);
                });
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public User updateUserProfile(Long userId, UpdateUserProfileCommandRequest command) {
        User user = getUserOrThrow(userId);
        String firstName = command.getFirstName();
        String lastName = command.getLastName();
        boolean namePartsProvided = firstName != null || lastName != null;
        if (namePartsProvided) {
            NameParts parts = resolveNameParts(firstName, lastName, null);
            log.info("Updating user profile in user-backend for user ID: {}", userId);
            userProfileGrpcClient.createUserProfile(userId, parts.firstName(), parts.lastName(), command.getEmail());
        }
        if (command.getMobile() != null && !command.getMobile().isBlank()) {
            String normalizedMobile = MobileNumberUtils.normalize(command.getMobile());
            if (normalizedMobile != null) {
                if (!normalizedMobile.equals(user.getMobile())) {
                    java.util.Optional<User> existing = userRepository.findFirstByMobile(normalizedMobile);
                    if (existing.isPresent() && !existing.get().getId().equals(userId)) {
                        throw new ConflictException("error.service.mobile_already_in_use", "DUPLICATE_MOBILE");
                    }
                    user.setMobile(normalizedMobile);
                }
            }
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
    public az.fitnest.identity.dto.response.OtpSendResponse requestEmailChange(Long userId, String newEmail) {
        User user = getUserOrThrow(userId);
        String trimmedEmail = (newEmail != null) ? newEmail.trim().toLowerCase() : null;

        try {
            var currentProfile = userProfileGrpcClient.getUserProfileDetails(userId);
            if (currentProfile != null && trimmedEmail != null && trimmedEmail.equals(currentProfile.getEmail())) {
                throw new az.fitnest.identity.exception.ValidationException("error.service.same_email", "SAME_EMAIL");
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to fetch profile during email change request for user {}. Proceeding without same-email check.",
                    userId);
        }

        boolean alreadyExists = trimmedEmail != null && userProfileGrpcClient.getUserByEmail(trimmedEmail) != null;
        if (alreadyExists) {
            throw new ConflictException("error.service.email_already_in_use", "DUPLICATE_EMAIL");
        }

        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.EMAIL_CHANGE, null, newEmail, null);
        return otpService.sendOtpByUserId(userId, otpRequest);
    }

    @Override
    @Transactional
    public User confirmEmailChange(Long userId, String otpSessionId, String otpCode) {
        User user = getUserOrThrow(userId);
        var verificationResult = otpService.verifyOtp(otpSessionId, otpCode);
        if (verificationResult.purpose() != OtpPurpose.EMAIL_CHANGE) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException(
                    "error.service.invalid_operation_context");
        }
        String newEmail = verificationResult.email();
        log.info("Confirming email change to {} in user-backend for user ID: {}", newEmail, userId);
        userProfileGrpcClient.createUserProfile(userId, null, null, newEmail.trim().toLowerCase());
        User saved = userRepository.save(user);
        localEventPublisher.publishEvent(new UserUpdatedEvent(userId));
        return saved;
    }

    @Override
    @Transactional
    public az.fitnest.identity.dto.response.OtpSendResponse requestMobileChange(Long userId, String newMobile) {
        User user = getUserOrThrow(userId);
        String normalizedMobile = MobileNumberUtils.normalize(newMobile);

        if (normalizedMobile == null) {
            throw new az.fitnest.identity.exception.ValidationException("error.validation", "INVALID_MOBILE");
        }

        if (normalizedMobile.equals(user.getMobile())) {
            throw new az.fitnest.identity.exception.ValidationException("error.service.same_mobile", "SAME_MOBILE");
        }

        if (userRepository.findFirstByMobile(normalizedMobile).isPresent()) {
            throw new ConflictException("error.service.mobile_already_in_use", "DUPLICATE_MOBILE");
        }

        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.MOBILE_CHANGE, normalizedMobile, null, null);
        return otpService.sendOtpByUserId(userId, otpRequest);
    }

    @Override
    @Transactional
    public User confirmMobileChange(Long userId, String otpSessionId, String otpCode) {
        User user = getUserOrThrow(userId);
        var verificationResult = otpService.verifyOtp(otpSessionId, otpCode);

        if (verificationResult.purpose() != OtpPurpose.MOBILE_CHANGE) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException(
                    "error.service.invalid_operation_context");
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
        if (language != null && language.equalsIgnoreCase(user.getLanguage())) {
            return user;
        }
        userRepository.updateLanguage(userId, language);
        user.setLanguage(language);
        return user;
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public void deactivateAccount(Long userId, az.fitnest.identity.dto.request.DeactivateAccountRequest request) {
        String reason = (request != null) ? request.getReason() : null;
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
        publishUserEvent("USER_UPDATED", userId);
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

        boolean isEligible = user.getMobile() != null && !user.getMobile().trim().isEmpty();
        if (!isEligible) {
            throw new az.fitnest.identity.exception.BadRequestException("error.auth.social_only_account");
        }

        if (user.isHasLocalPassword()) {
            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                throw new az.fitnest.identity.exception.ValidationException("error.validation.missing_field", "MISSING_FIELD");
            }
            if (!passwordService.verifyPassword(oldPassword, user.getPasswordHash()).matches()) {
                throw new az.fitnest.identity.exception.BadRequestException("error.auth.invalid_credentials", "error.auth.invalid_credentials");
            }
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
        user.setHasLocalPassword(true);
        userRepository.save(user);
        localEventPublisher.publishEvent(new PasswordChangedEvent(userId));
    }

    @Transactional
    @Override
    public void resetUserPasswordDirectly(Long userId, String newPassword) {
        User user = getUserById(userId);
        if (newPassword.length() < 8) {
            throw new az.fitnest.identity.exception.ValidationException("error.validation", "WEAK_PASSWORD");
        }
        user.setPasswordHash(passwordService.hashPassword(newPassword));
        user.setHasLocalPassword(true);
        if (user.getStatus() == UserStatus.INACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }
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
        if (v.isEmpty())
            return null;
        java.util.regex.Matcher matcher = NAME_PART_PATTERN.matcher(v);
        if (matcher.matches()) {
            return v;
        } else {
            throw new az.fitnest.identity.exception.ValidationException("error.validation", "INVALID_NAME_CHARACTERS");
        }
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
                .map(userResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> searchUsers(int page, int size, Long id, String name, String surname, String email,
                                          String mobile) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);
        return userRepository.searchUsers(id, name, surname, email, mobile, pageable)
                .map(userResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> searchUsersAdvanced(int page, int size, String query, Long packageID,
                                                  Integer durationMonths) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);

        QueryParams params = parseQuery(query);
        List<Long> subscriptionUserIds = resolveSubscriptionFilteredIds(packageID, durationMonths, null);

        if (subscriptionUserIds != null && subscriptionUserIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<User> userPage = userRepository.searchUsersAdvanced(
                params.id(), params.name(), params.surname(), params.email(), params.mobile(),
                subscriptionUserIds, null, pageable);

        return userPage.map(userResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<az.fitnest.identity.dto.response.AdminUserResponse> getAdminUsers(
            int page, int size, String query, Long packageID, Integer durationMonths, String type, String roles) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);

        QueryParams params = parseQuery(query);
        List<Long> subscriptionUserIds = resolveSubscriptionFilteredIds(packageID, durationMonths, type);

        if (subscriptionUserIds != null && subscriptionUserIds.isEmpty()) {
            return Page.empty(pageable);
        }

        String roleName = (roles != null && !roles.isBlank()) ? roles.strip() : null;

        Page<User> userPage = userRepository.searchUsersAdvanced(
                params.id(), params.name(), params.surname(), params.email(), params.mobile(),
                subscriptionUserIds, roleName, pageable);

        return userPage.map(user -> {
            String subscriptionStatus = null;
            try {
                var sub = userSubscriptionGrpcClient.getActiveSubscription(user.getId());
                subscriptionStatus = (sub != null && !sub.getSubscriptionStatus().isEmpty())
                        ? sub.getSubscriptionStatus()
                        : null;
            } catch (Exception e) {
                log.warn("Failed to fetch subscription for user {}: {}", user.getId(), e.getMessage());
            }
            return new az.fitnest.identity.dto.response.AdminUserResponse(
                    user.getId(), null, null, user.getMobile(), null,
                    user.getStatus() != null ? user.getStatus().name() : null,
                    subscriptionStatus);
        });
    }

    private List<Long> resolveSubscriptionFilteredIds(Long packageID, Integer durationMonths, String type) {
        List<Long> ids = null;
        if (packageID != null) {
            ids = intersect(ids, userSubscriptionGrpcClient.getUserIdsByPackageId(packageID));
        }
        if (durationMonths != null) {
            ids = intersect(ids, userSubscriptionGrpcClient.getUserIdsByDurationMonths(durationMonths));
        }
        if (type != null && !type.isBlank()) {
            ids = intersect(ids, userSubscriptionGrpcClient.getUserIdsByType(type));
        }
        return ids;
    }

    private List<Long> intersect(List<Long> list1, List<Long> list2) {
        if (list1 == null)
            return list2;
        if (list2 == null)
            return list1;
        list1.retainAll(list2);
        return list1;
    }

    private QueryParams parseQuery(String query) {
        Long id = null;
        String name = null, surname = null, email = null, mobile = null;
        if (query != null && !query.isBlank()) {
            if (!query.contains("=")) {
                String generic = query.trim();
                name = surname = email = mobile = generic;
            } else {
                for (String part : query.split(";")) {
                    String[] kv = part.split("=", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().toLowerCase();
                        String value = kv[1].trim();
                        switch (key) {
                            case "id" -> {
                                try {
                                    id = Long.parseLong(value);
                                } catch (NumberFormatException ignored) {
                                }
                            }
                            case "name" -> name = value;
                            case "surname" -> surname = value;
                            case "email" -> email = value;
                            case "mobile" -> mobile = value;
                        }
                    }
                }
            }
        }
        return new QueryParams(id, name, surname, email, mobile);
    }

    @Override
    @Transactional
    public az.fitnest.identity.dto.response.OtpSendResponse resendMobileChangeOtp(Long userId, String otpSessionId) {
        User user = getUserOrThrow(userId);
        var session = otpService.getOtpSession(otpSessionId);
        if (session.purpose() != az.fitnest.identity.model.enums.OtpPurpose.MOBILE_CHANGE) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException(
                    "error.service.invalid_operation_context");
        }
        return otpService.resendOtp(otpSessionId, az.fitnest.identity.model.enums.OtpPurpose.MOBILE_CHANGE);
    }

    @Override
    @Transactional
    public az.fitnest.identity.dto.response.OtpSendResponse resendEmailChangeOtp(Long userId, String otpSessionId) {
        User user = getUserOrThrow(userId);
        var session = otpService.getOtpSession(otpSessionId);
        if (session.purpose() != az.fitnest.identity.model.enums.OtpPurpose.EMAIL_CHANGE) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException(
                    "error.service.invalid_operation_context");
        }
        return otpService.resendOtp(otpSessionId, az.fitnest.identity.model.enums.OtpPurpose.EMAIL_CHANGE);
    }

    @Override
    public az.fitnest.identity.dto.response.OtpSendResponse sendOtp(
            az.fitnest.identity.dto.request.OtpSendRequest request) {
        if (request.getPurpose() == az.fitnest.identity.model.enums.OtpPurpose.MOBILE_CHANGE) {
            return otpService.sendOtp(request);
        } else {
            return otpService.sendOtp(request);
        }
    }

    private record QueryParams(Long id, String name, String surname, String email, String mobile) {
    }

    private record UserEvent(String eventType, Long userId, long timestamp) {
    }

    private record UserUpdatedEvent(Long userId) {
    }

    private record PasswordChangedEvent(Long userId) {
    }

    private record UserAccountDeletedEventLocal(Long userId) {
    }

    private record UserSetupCompletedEventLocal(Long userId) {
    }

    private record NameParts(String firstName, String lastName) {
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void blockUser(Long userId) {
        User user = getUserOrThrow(userId);
        if (testUserHelper.isTestUser(user)) {
            throw new az.fitnest.identity.exception.ForbiddenException("Cannot block test user", "CANNOT_BLOCK_TEST_USER");
        }
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);

        // Terminate all sessions
        redisTokenService.removeAllSessions(userId);
        authTokenRepository.deleteByUserId(userId);

        publishUserEvent("USER_UPDATED", userId);
        log.info("User {} has been BLOCKED by admin", userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void unblockUser(Long userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        publishUserEvent("USER_UPDATED", userId);
        log.info("User {} has been UNBLOCKED by admin", userId);
    }

    @Override
    public List<az.fitnest.identity.dto.response.RoleResponse> getAvailableRoles() {
        return List.of(
                new az.fitnest.identity.dto.response.RoleResponse("ROLE_ADMIN", "Sistem admini"),
                new az.fitnest.identity.dto.response.RoleResponse("ROLE_FITNEST_STAFF", "Fitnest Komandası"),
                new az.fitnest.identity.dto.response.RoleResponse("ROLE_GYM_SUPER_ADMIN", "Zal Super Admini"),
                new az.fitnest.identity.dto.response.RoleResponse("ROLE_GYM_ADMIN", "Zal Admini"),
                new az.fitnest.identity.dto.response.RoleResponse("ROLE_USER", "Müştəri")
        );
    }

    @Override
    public void changeUserRole(Long userId, String roleName) {
        updateUserRole(userId, roleName);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void hardDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found", "RESOURCE_NOT_FOUND"));
        if (testUserHelper.isTestUser(user)) {
            throw new az.fitnest.identity.exception.ForbiddenException("Cannot delete test user", "CANNOT_DELETE_TEST_USER");
        }

        Long id = user.getId();

        redisTokenService.removeAllSessions(id);

        authTokenRepository.deleteByUserId(id);

        userDeviceRepository.deleteByUserId(id);

        userRepository.deleteById(id);

        publishUserEvent("USER_HARD_DELETED", id);

        log.warn("User {} has been PERMANENTLY DELETED by admin", id);
    }
}
