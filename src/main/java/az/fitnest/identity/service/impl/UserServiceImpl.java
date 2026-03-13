package az.fitnest.identity.service.impl;

import az.fitnest.identity.client.PackageGrpcClient;
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
import az.fitnest.identity.util.TokenHasher;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenRepository authTokenRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTokenService redisTokenService;
    private final IdentityEventPublisher eventPublisher;
    private final ApplicationEventPublisher localEventPublisher;
    private final PasswordService passwordService;
    private final OtpService otpService;
    private final PackageGrpcClient packageGrpcClient;

    @Transactional
    @Override
    public User updateUserRole(Long userId, String roleName) {
        User user = getUserById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found", "RESOURCE_NOT_FOUND"));

        user.setRole(role);

        return userRepository.save(user);
    }

    // Removed @Cacheable to always fetch latest user data
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
        return createNewUserInternal(normalizeNamePart(firstName), normalizeNamePart(lastName), passwordHash, mobile);
    }

    @Transactional
    @Override
    public User createNewUserWithFullName(String fullName, String passwordHash, String mobile) {
        NameParts nameParts = splitFullName(fullName);
        return createNewUserInternal(nameParts.firstName(), nameParts.lastName(), passwordHash, mobile);
    }

    private User createNewUserInternal(String firstName, String lastName, String passwordHash, String mobile) {
        mobile = MobileNumberUtils.normalize(mobile);
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .passwordHash(passwordHash)
                .mobile(mobile)
                .hasAccount(true)
                .setupRequired(true)
                .failedLoginAttempts(0)
                .status(UserStatus.ACTIVE)
                .role(roleRepository.findByName("ROLE_USER").orElse(null))
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
        publishUserEvent("USER_UPDATED", userId);
        return saved;
    }

    @Override
    @Transactional
    public az.fitnest.identity.dto.OtpSendResponse requestEmailChange(Long userId, String newEmail) {
        User user = getUserOrThrow(userId);
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new ConflictException("error.resource.conflict", "RESOURCE_CONFLICT");
        }
        if (userRepository.findFirstByEmail(newEmail.toLowerCase()).isPresent()) {
            throw new ConflictException("error.service.operation_not_allowed", "DUPLICATE_EMAIL");
        }

        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.EMAIL_CHANGE, null, newEmail);
        return otpService.sendOtpByUserId(userId, otpRequest);
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

        user.setEmail(newEmail.toLowerCase());
        User saved = userRepository.save(user);
        publishUserEvent("USER_UPDATED", userId);
        return saved;
    }

    @Override
    @Transactional
    public az.fitnest.identity.dto.OtpSendResponse requestMobileChange(Long userId, String newMobile) {
        User user = getUserOrThrow(userId);
        String normalizedMobile = MobileNumberUtils.normalize(newMobile);
        if (normalizedMobile.equals(user.getMobile())) {
            throw new ConflictException("error.resource.conflict", "RESOURCE_CONFLICT");
        }
        if (userRepository.findFirstByMobile(normalizedMobile).isPresent()) {
            throw new ConflictException("error.service.operation_not_allowed", "DUPLICATE_MOBILE");
        }

        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.MOBILE_CHANGE, normalizedMobile, null);
        return otpService.sendOtpByUserId(userId, otpRequest);
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
        }
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public User updateLanguage(Long userId, String language) {
        User user = getUserOrThrow(userId);
        user.setLanguage(language);
        return userRepository.save(user);
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
    public void deleteInactiveAccountsAfter30Days() {
        Instant threshold = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        java.util.List<Long> userIds = userRepository.findInactiveUserIds(threshold);
        for (Long userId : userIds) {
            try {
                deleteAccount(userId);
            } catch (Exception ex) {
                log.error("Failed to delete inactive account for userId {}", userId, ex);
            }
        }
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
        user.setPasswordHash(passwordService.hashPassword(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(UserStatus.INACTIVE);
        user.setInactiveAt(java.time.Instant.now());
        userRepository.save(user);
        publishUserEvent("ACCOUNT_DEACTIVATED", userId);
        redisTokenService.removeAllSessions(userId);
        authTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    @Override
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("error.resource.not_found", "RESOURCE_NOT_FOUND"));
        // Prevent deletion if role is assigned to any user
        if (userRepository.existsByRole(role)) {
            throw new ConflictException("error.role.in_use", "ROLE_IN_USE");
        }
        roleRepository.deleteById(roleId);
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
        return v.isEmpty() ? null : v;
    }

    private void publishUserEvent(String eventType, Long userId) {
        Map<String, Object> event = Map.of(
                "eventType", eventType,
                "userId", userId,
                "timestamp", System.currentTimeMillis()
        );
        kafkaTemplate.send("user-events", event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish user event", ex);
                }
            });
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> getAllUsersMapped(int page, int size) {
        return userRepository.findAll(PageRequest.of(Math.max(0, page - 1), size))
                .map(UserResponseMapper::toResponse);
    }

    @Transactional
    @Override
    public User updateSessionStatus(Long userId, az.fitnest.identity.model.enums.SessionStatus sessionStatus) {
        User user = getUserById(userId);
        user.setSessionStatus(sessionStatus);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> searchUsers(int page, int size, Long id, String name, String surname, String email, String mobile, Long packageID) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);
        if (packageID != null) {
            List<Long> userIds = packageGrpcClient.getUserIdsByPackageId(packageID);
            if (userIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return userRepository.findByIdIn(userIds, pageable)
                    .map(UserResponseMapper::toResponse);
        }
        return userRepository.searchUsers(id, name, surname, email, mobile, null, pageable)
                .map(UserResponseMapper::toResponse);
    }

    private record UserSetupCompletedEventLocal(Long userId) {
    }

    private record NameParts(String firstName, String lastName) {
    }
}
