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
import az.fitnest.identity.service.SocialAuthService;
import az.fitnest.identity.service.TokenIssuanceService;
import az.fitnest.identity.util.DeviceDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    @Override
    public LoginResponse socialLoginGoogle(GoogleSocialRequest request) {
        GoogleTokenVerifier.GoogleTokenClaims claims = googleTokenVerifier.verify(request.idToken());

        return processSocialLogin(
                SocialProvider.GOOGLE,
                claims.userId(),
                claims.email(),
                claims.givenName(),
                claims.familyName(),
                claims.name()
        );
    }

    @Transactional
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
                fullName
        );
    }

    private LoginResponse processSocialLogin(SocialProvider provider, String providerId, String email,
                                            String firstName, String lastName, String fullName) {
        Optional<SocialAuth> existingSocialAuth = socialAuthRepository.findByProviderAndProviderId(provider, providerId);

        if (existingSocialAuth.isPresent()) {
            SocialAuth socialAuth = existingSocialAuth.get();
            User user = userRepository.findById(socialAuth.getUserId())
                    .orElseThrow(() -> new InvalidCredentialsException("error.auth.invalid_credentials"));

            handleUserStatus(user);
            return tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());
        }

        if (email != null && !email.isEmpty()) {
            Optional<User> userByEmail = userRepository.findFirstByEmail(email);
            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                handleUserStatus(user);
                linkSocialAccount(user.getId(), provider, providerId);
                return tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());
            }
        }

        User newUser = createUserForSocialLogin(firstName, lastName, fullName, email, null);
        linkSocialAccount(newUser.getId(), provider, providerId);

        return tokenIssuanceService.issueTokens(newUser, DeviceDetector.detectDeviceType());
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

    private void linkSocialAccount(Long userId, SocialProvider provider, String providerId) {
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

    private User createUserForSocialLogin(String firstName, String lastName, String fullName, String email, String mobile) {
        NameParts nameParts = resolveNameParts(firstName, lastName, fullName);

        User user = User.builder()
                .firstName(nameParts.firstName())
                .lastName(nameParts.lastName())
                .email(email)
                .mobile(mobile)
                .passwordHash(null)
                .hasAccount(true)
                .setupRequired(true)
                .failedLoginAttempts(0)
                .status(UserStatus.ACTIVE)
                .role(roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new IllegalStateException("System error: Default role ROLE_USER not found")))
                .build();

        return userRepository.save(user);
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
