package az.fitnest.identity.service.impl;

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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenRepository authTokenRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTokenService redisTokenService;
    private final IdentityEventPublisher eventPublisher;
    private final ApplicationEventPublisher localEventPublisher;
    private final PasswordService passwordService;
    private final OtpService otpService;

    @Transactional
    @Override
    public User updateUserRole(Long userId, String roleName) {
        User user = getUserById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rol tapılmadı: " + roleName));

        user.setRole(role);

        return userRepository.save(user);
    }

    @Cacheable(value = "users", key = "#userId")
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
        if (mobile != null && userRepository.findFirstByMobile(mobile).isPresent()) {
            throw new ConflictException("Bu mobil nömrə artıq qeydiyyatdan keçib");
        }

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

        return userRepository.save(user);
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
    public void requestEmailChange(Long userId, String newEmail) {
        User user = getUserOrThrow(userId);
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new ConflictException("Yeni e-poçt köhnə ilə eynidir");
        }
        if (userRepository.findFirstByEmail(newEmail.toLowerCase()).isPresent()) {
            throw new ConflictException("Bu e-poçt artıq qeydiyyatdan keçib");
        }

        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.EMAIL_CHANGE, null, newEmail);
        otpService.sendOtp(otpRequest);
    }

    @Override
    @Transactional
    public User confirmEmailChange(Long userId, String newEmail, String otpCode) {
        User user = getUserOrThrow(userId);
        otpService.verifyOtpByIdentifier(newEmail.toLowerCase(), OtpPurpose.EMAIL_CHANGE, otpCode);

        user.setEmail(newEmail.toLowerCase());
        User saved = userRepository.save(user);
        publishUserEvent("USER_UPDATED", userId);
        return saved;
    }

    @Override
    @Transactional
    public void requestMobileChange(Long userId, String newMobile) {
        User user = getUserOrThrow(userId);
        String normalizedMobile = MobileNumberUtils.normalize(newMobile);
        if (normalizedMobile.equals(user.getMobile())) {
            throw new ConflictException("Yeni mobil nömrə köhnə ilə eynidir");
        }
        if (userRepository.findFirstByMobile(normalizedMobile).isPresent()) {
            throw new ConflictException("Bu mobil nömrə artıq qeydiyyatdan keçib");
        }

        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.MOBILE_CHANGE, normalizedMobile, null);
        otpService.sendOtp(otpRequest);
    }

    @Override
    @Transactional
    public User confirmMobileChange(Long userId, String newMobile, String otpCode) {
        User user = getUserOrThrow(userId);
        String normalizedMobile = MobileNumberUtils.normalize(newMobile);
        otpService.verifyOtpByIdentifier(normalizedMobile, OtpPurpose.MOBILE_CHANGE, otpCode);

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
            // Silently ignore or handle via other means as requested (no logging)
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
    public void deactivateAccount(Long userId) {
        deleteUser(userId, "Self-deactivation");
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public void deleteUser(Long userId, String reason) {
        User user = getUserOrThrow(userId);
        // Soft-delete: set status to INACTIVE and persist
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        List<AuthToken> tokens = authTokenRepository.findByUserId(userId);
        for (AuthToken token : tokens) {
            if (token.getJti() != null) {
                redisTokenService.revokeAccessToken(token.getJti());
            }
        }
        authTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    @Override
    public void deleteAllUsers() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getRole() != null && "ROLE_SUPER_ADMIN".equals(user.getRole().getName())) {
                continue;
            }
            deleteUser(user.getId(), "Super Admin Cleanup");
        }
    }

    @Transactional
    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmNewPassword) {
        User user = getUserById(userId);
        if (!passwordService.verifyPassword(oldPassword, user.getPasswordHash()).matches()) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException("Köhnə şifrə yanlışdır");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException("Yeni şifrələr uyğun gəlmir");
        }
        user.setPasswordHash(passwordService.hashPassword(newPassword));
        userRepository.save(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı"));
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
        try {
            kafkaTemplate.send("user-events", event);
        } catch (Exception e) {
            // Silently ignore or handle via other means as requested (no logging)
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> getAllUsersMapped(int page, int size) {
        return userRepository.findAll(PageRequest.of(page - 1, size))
                .map(UserResponseMapper::toResponse);
    }

    @Transactional
    @Override
    public User updateSessionStatus(Long userId, az.fitnest.identity.model.enums.SessionStatus sessionStatus) {
        User user = getUserById(userId);
        user.setSessionStatus(sessionStatus);
        return userRepository.save(user);
    }

    private record UserSetupCompletedEventLocal(Long userId) {
    }

    private record NameParts(String firstName, String lastName) {
    }
}
