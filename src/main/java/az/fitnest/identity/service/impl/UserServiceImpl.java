package az.fitnest.identity.service.impl;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;

import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.entity.AuthToken;
import az.fitnest.identity.service.EmailService;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.exception.ResourceNotFoundException;
import az.fitnest.identity.dto.UpdateUserProfileCommand;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.constants.RoleName;
import az.fitnest.identity.entity.Role;
import az.fitnest.identity.entity.User;
import az.fitnest.identity.service.impl.IdentityEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IdentityEventPublisher eventPublisher;

    @Transactional
        @Override
    public User updateUserRole(Long userId, RoleName roleName) {
        User user = getUserById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        
        user.setRole(role);
        
        return userRepository.save(user);
    }
    private final AuthTokenRepository authTokenRepository;
    private final RedisTokenService redisTokenService;
    private final EmailService emailService;

    @org.springframework.cache.annotation.Cacheable(value = "users", key = "#userId")
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
        mobile = az.fitnest.identity.criteria.MobileNumberUtils.normalize(mobile);
        if (mobile != null && userRepository.findByMobileIncludingDeleted(mobile).isPresent()) {
            throw new ConflictException("Mobile number already registered");
        }

        User user = User.builder()
                .firstName(normalizeNamePart(firstName))
                .lastName(normalizeNamePart(lastName))
                .passwordHash(passwordHash)
                .mobile(mobile)
                .hasAccount(true)
                .setupRequired(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .isDeleted(false)
                .role(roleRepository.findByName(az.fitnest.identity.constants.RoleName.ROLE_USER).orElse(null))
                .build();

        return userRepository.save(user);
    }

    @Transactional
        @Override
    public User createNewUserWithFullName(String fullName, String passwordHash, String mobile) {
        mobile = az.fitnest.identity.criteria.MobileNumberUtils.normalize(mobile);
        NameParts nameParts = splitFullName(fullName);
        User user = User.builder()
                .firstName(nameParts.firstName())
                .lastName(nameParts.lastName())
                .passwordHash(passwordHash)
                .mobile(mobile)
                .hasAccount(true)
                .setupRequired(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .isDeleted(false)
                .role(roleRepository.findByName(az.fitnest.identity.constants.RoleName.ROLE_USER).orElse(null))
                .build();

        return userRepository.save(user);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#userId")
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

        if (command.email() != null && !command.email().isEmpty()) {
            user.setEmail(command.email());
        }

        User saved = userRepository.save(user);
        publishUserEvent("USER_UPDATED", userId);
        return saved;
    }

    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#userId")
    @Transactional
        @Override
    public User updateProfileImageUrl(Long userId, String profileImageUrl) {
        User user = getUserOrThrow(userId);

        user.setProfileImageUrl(profileImageUrl);
        User saved = userRepository.save(user);
        publishUserEvent("USER_UPDATED", userId);
        return saved;
    }

    private final org.springframework.context.ApplicationEventPublisher localEventPublisher;

    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#userId")
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

    @org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void handleUserSetupCompleted(UserSetupCompletedEventLocal event) {
        try {
            eventPublisher.publishSetupCompleted(event.userId());
        } catch (Exception e) {
        }
    }

    private record UserSetupCompletedEventLocal(Long userId) {}

    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#userId")
    @Transactional
        @Override
    public User updateLanguage(Long userId, az.fitnest.identity.constants.Language language) {
        User user = getUserOrThrow(userId);
        user.setLanguage(language);
        return userRepository.save(user);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#userId")
    @Transactional
        @Override
    public void deleteUser(Long userId, String reason) {
        User user = getUserOrThrow(userId);



        userRepository.delete(user);

        java.util.List<AuthToken> tokens = authTokenRepository.findByUserId(userId);
        for (AuthToken token : tokens) {
            redisTokenService.revokeAccessToken(token.getAccessToken());
        }
        authTokenRepository.deleteByUserId(userId);

    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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

    private record NameParts(String firstName, String lastName) {
    }

    private void publishUserEvent(String eventType, Long userId) {
        Map<String, Object> event = Map.of(
            "eventType", eventType,
            "userId", userId,
            "timestamp", System.currentTimeMillis()
        );
        kafkaTemplate.send("user-events", event);
    }
}
