package az.fitnest.iam.user.adapter.service;

import az.fitnest.iam.auth.adapter.persistence.AuthTokenRepository;
import az.fitnest.iam.auth.domain.model.AuthToken;
import az.fitnest.iam.messaging.EmailService;
import az.fitnest.iam.security.RedisTokenService;
import az.fitnest.iam.shared.exception.ConflictException;
import az.fitnest.iam.shared.exception.ResourceNotFoundException;
import az.fitnest.iam.user.application.command.UpdateUserProfileCommand;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final RedisTokenService redisTokenService;
    private final EmailService emailService;

    @org.springframework.cache.annotation.Cacheable(value = "users", key = "#userId")
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return getUserOrThrow(userId);
    }

    @Transactional
    public User createNewUser(String firstName, String lastName, String passwordHash, String mobile) {
        mobile = az.fitnest.iam.shared.util.MobileNumberUtils.normalize(mobile);
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
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User createNewUserWithFullName(String fullName, String passwordHash, String mobile) {
        mobile = az.fitnest.iam.shared.util.MobileNumberUtils.normalize(mobile);
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
                .build();

        return userRepository.save(user);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#userId")
    @Transactional
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

        return userRepository.save(user);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#userId")
    @Transactional
    public User updateProfileImageUrl(Long userId, String profileImageUrl) {
        User user = getUserOrThrow(userId);

        user.setProfileImageUrl(profileImageUrl);
        return userRepository.save(user);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#userId")
    @Transactional
    public User updateSetupRequired(Long userId, boolean setupRequired) {
        User user = getUserOrThrow(userId);
        user.setSetupRequired(setupRequired);
        return userRepository.save(user);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#userId")
    @Transactional
    public User updateLanguage(Long userId, az.fitnest.iam.user.domain.enums.Language language) {
        User user = getUserOrThrow(userId);
        user.setLanguage(language);
        return userRepository.save(user);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "users", key = "#userId")
    @Transactional
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
}
