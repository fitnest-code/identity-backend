package az.fitnest.identity.service.impl;

import az.fitnest.identity.dto.request.AppleSocialRequest;
import az.fitnest.identity.dto.request.GoogleSocialRequest;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.model.entity.SocialAuth;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.enums.SocialProvider;
import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.SocialAuthRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.AppleTokenVerifier;
import az.fitnest.identity.service.GoogleTokenVerifier;
import az.fitnest.identity.service.LegalService;
import az.fitnest.identity.service.SocialAuthService;
import az.fitnest.identity.service.TokenIssuanceService;
import az.fitnest.identity.service.UserProfileGrpcClient;
import az.fitnest.identity.util.DeviceDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAuthServiceImpl implements SocialAuthService {

    private final UserRepository userRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppleTokenVerifier appleTokenVerifier;
    private final TokenIssuanceService tokenIssuanceService;
    private final RoleRepository roleRepository;
    private final UserProfileGrpcClient userProfileGrpcClient;
    private final LegalService legalService;

    @Autowired
    @Lazy
    private SocialAuthServiceImpl self;

    @Override
    public LoginResponse socialLoginGoogle(GoogleSocialRequest request) {
        log.info("Starting Google social login process");
        GoogleTokenVerifier.GoogleTokenClaims claims = googleTokenVerifier.verify(request.idToken());
        log.info("Google token verified successfully for email: {}", claims.email());

        return processSocialLogin(
                SocialProvider.GOOGLE,
                claims.userId(),
                claims.email(),
                claims.givenName(),
                claims.familyName(),
                claims.name(),
                claims.picture()
        );
    }

    @Override
    public LoginResponse socialLoginApple(AppleSocialRequest request) {
        AppleTokenVerifier.AppleTokenClaims claims = appleTokenVerifier.verify(request.identityToken());

        String firstName = claims.firstName() != null ? claims.firstName() : request.firstName();
        String lastName = claims.lastName() != null ? claims.lastName() : request.lastName();
        String fullName = request.fullName() != null ? request.fullName() : "User";

        return processSocialLogin(
                SocialProvider.APPLE,
                claims.userId(),
                claims.email(),
                firstName,
                lastName,
                fullName,
                null
        );
    }

    private LoginResponse processSocialLogin(SocialProvider provider, String providerId, String email,
                                             String firstName, String lastName, String fullName, String pictureUrl) {
        log.info("Processing social login for provider: {}, providerId: {}, email: {}", provider, providerId, email);
        Optional<SocialAuth> existingSocialAuth = socialAuthRepository.findByProviderAndProviderId(provider, providerId);

        if (existingSocialAuth.isPresent()) {
            SocialAuth socialAuth = existingSocialAuth.get();
            log.info("Found existing social auth record for user ID: {}", socialAuth.getUserId());
            User user = self.findAndReactivateUser(socialAuth.getUserId());

            var profile = userProfileGrpcClient.getUserProfileDetails(user.getId());
            if (profile == null || profile.getProfileImageUrl() == null || profile.getProfileImageUrl().isBlank()) {
                log.info("Updating profile image for existing user: {}", user.getId());
                userProfileGrpcClient.updateProfileImage(user.getId(), pictureUrl);
            }
            log.info("Issuing tokens for existing user: {}", user.getId());
            return tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());
        }

        if (email != null && !email.isEmpty()) {
            log.info("No social auth record found, checking if user exists by email in user-backend: {}", email);
            var userByEmail = userProfileGrpcClient.getUserByEmail(email);
            if (userByEmail != null) {
                Long userId = userByEmail.userId();
                log.info("Found existing user by email in user-backend: {}. UserId: {}. Linking {} account.", email, userId, provider);
                User user = self.findAndReactivateUser(userId);

                var profile = userProfileGrpcClient.getUserProfileDetails(user.getId());
                if (profile == null || profile.getProfileImageUrl() == null || profile.getProfileImageUrl().isBlank()) {
                    log.info("Updating profile image for existing user (by email): {}", user.getId());
                    userProfileGrpcClient.updateProfileImage(user.getId(), pictureUrl);
                }
                self.linkSocialAccount(user.getId(), provider, providerId);
                return tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());
            }
        }

        log.info("No existing user found. Creating new user for social login.");
        User newUser = self.createIdentityUser();
        NameParts nameParts = resolveNameParts(firstName, lastName, fullName);
        log.info("Creating user profile in user-backend for user ID: {}", newUser.getId());
        userProfileGrpcClient.createUserProfile(newUser.getId(), nameParts.firstName(), nameParts.lastName(), email);

        if (pictureUrl != null && !pictureUrl.isBlank()) {
            userProfileGrpcClient.updateProfileImage(newUser.getId(), pictureUrl);
        }
        self.linkSocialAccount(newUser.getId(), provider, providerId);

        log.info("Issuing tokens for new user: {}", newUser.getId());
        legalService.autoAcceptLatestConsents(newUser.getId());
        return tokenIssuanceService.issueTokens(newUser, DeviceDetector.detectDeviceType());
    }

    @Transactional
    public User findAndReactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User ID {} not found in users table", userId);
                    return new InvalidCredentialsException("error.auth.invalid_credentials");
                });
        handleUserStatus(user);
        return user;
    }

    @Transactional
    public User createIdentityUser() {
        User user = User.builder()
                .mobile(null)
                .passwordHash(null)
                .hasAccount(true)
                .setupRequired(true)
                .failedLoginAttempts(0)
                .status(UserStatus.ACTIVE)
                .hasLocalPassword(false)
                .role(roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new IllegalStateException("System error: Default role ROLE_USER not found")))
                .build();

        return userRepository.save(user);
    }

    private void handleUserStatus(User user) {
        switch (user.getStatus()) {
            case ACTIVE -> {
            }
            case INACTIVE -> {
                log.info("Reactivating INACTIVE user: {}", user.getId());
                user.setStatus(UserStatus.ACTIVE);
                user.setInactiveAt(null);
                userRepository.save(user);
            }
            case LOCKED -> throw new UnauthorizedException("error.auth.account_locked");
            case DELETED -> throw new UnauthorizedException("error.auth.account_deleted");
            default -> throw new UnauthorizedException("error.auth.invalid_status");
        }
    }

    @Transactional
    public void linkSocialAccount(Long userId, SocialProvider provider, String providerId) {
        try {
            SocialAuth socialAuth = SocialAuth.builder()
                    .userId(userId)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            socialAuthRepository.save(socialAuth);
        } catch (Exception e) {
            log.warn("Potential race condition during social account linking for user {}: {}", userId, e.getMessage());
            Optional<SocialAuth> existing = socialAuthRepository.findByProviderAndProviderId(provider, providerId);
            if (existing.isEmpty()) {
                throw new IllegalStateException("Failed to link social account and no existing link found", e);
            }
        }
    }

    private NameParts resolveNameParts(String firstName, String lastName, String fullName) {
        String fn = normalizeNamePart(firstName);
        String ln = normalizeNamePart(lastName);

        if (fn != null || ln != null) {
            return new NameParts(fn, ln);
        }

        return splitFullName(fullName);
    }

    private String normalizeNamePart(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.isEmpty() ? null : v;
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

    private record NameParts(String firstName, String lastName) {
    }
}
