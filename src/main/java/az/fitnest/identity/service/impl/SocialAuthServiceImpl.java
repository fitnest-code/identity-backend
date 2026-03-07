package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.service.AppleTokenVerifier;
import az.fitnest.identity.service.GoogleTokenVerifier;
import az.fitnest.identity.service.SocialAuthService;
import az.fitnest.identity.service.TokenIssuanceService;

import az.fitnest.identity.dto.AppleSocialRequest;
import az.fitnest.identity.dto.GoogleSocialRequest;
import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.repository.SocialAuthRepository;
import az.fitnest.identity.model.enums.SocialProvider;
import az.fitnest.identity.model.entity.SocialAuth;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.util.DeviceDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialAuthServiceImpl implements SocialAuthService {

    private final UserRepository userRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppleTokenVerifier appleTokenVerifier;
    private final TokenIssuanceService tokenIssuanceService;
    private final az.fitnest.identity.repository.RoleRepository roleRepository;

    @Transactional
    @Override
    public LoginResponse socialLoginApple(AppleSocialRequest request) {
        AppleTokenVerifier.AppleTokenClaims claims = appleTokenVerifier.verify(request.identityToken());
        String providerId = claims.userId();

        Optional<SocialAuth> existingSocialAuth = socialAuthRepository.findByProviderAndProviderId(
                SocialProvider.APPLE, providerId);

        if (existingSocialAuth.isPresent()) {
            SocialAuth socialAuth = existingSocialAuth.get();
            User user = userRepository.findById(socialAuth.getUserId())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

            if (user.isDeactivated()) {
                User newUser = createUserForSocialLogin(
                        request.firstName(),
                        request.lastName(),
                        request.fullName() != null ? request.fullName() : "User",
                        null
                );
                socialAuth.setUserId(newUser.getId());
                socialAuthRepository.save(socialAuth);
                return tokenIssuanceService.issueTokens(newUser, DeviceDetector.detectDeviceType());
            }

            return tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());
        }

        // No email linking anymore. Create new user.

        User newUser = createUserForSocialLogin(
                request.firstName(),
                request.lastName(),
                request.fullName() != null ? request.fullName() : "User",
                null);

        SocialAuth socialAuth = SocialAuth.builder()
                .userId(newUser.getId())
                .provider(SocialProvider.APPLE)
                .providerId(providerId)
                .build();
        socialAuthRepository.save(socialAuth);

        return tokenIssuanceService.issueTokens(newUser, DeviceDetector.detectDeviceType());
    }

    @Transactional
    @Override
    public LoginResponse socialLoginGoogle(GoogleSocialRequest request) {
        GoogleTokenVerifier.GoogleTokenClaims claims = googleTokenVerifier.verify(request.idToken());
        String providerId = claims.userId();

        Optional<SocialAuth> existingSocialAuth = socialAuthRepository.findByProviderAndProviderId(
                SocialProvider.GOOGLE, providerId);

        if (existingSocialAuth.isPresent()) {
            SocialAuth socialAuth = existingSocialAuth.get();
            User user = userRepository.findById(socialAuth.getUserId())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

            if (user.isDeactivated()) {
                User newUser = createUserForSocialLogin(
                        request.firstName(),
                        request.lastName(),
                        request.fullName() != null ? request.fullName() : "User",
                        null
                );
                socialAuth.setUserId(newUser.getId());
                socialAuthRepository.save(socialAuth);
                return tokenIssuanceService.issueTokens(newUser, DeviceDetector.detectDeviceType());
            }

            return tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());
        }

        // No email linking anymore. Create new user.

        User newUser = createUserForSocialLogin(
                request.firstName(),
                request.lastName(),
                request.fullName() != null ? request.fullName() : "User",
                null);

        SocialAuth socialAuth = SocialAuth.builder()
                .userId(newUser.getId())
                .provider(SocialProvider.GOOGLE)
                .providerId(providerId)
                .build();
        socialAuthRepository.save(socialAuth);

        return tokenIssuanceService.issueTokens(newUser, DeviceDetector.detectDeviceType());
    }

    private User createUserForSocialLogin(String firstName, String lastName, String fullName, String mobile) {
        NameParts nameParts = resolveNameParts(firstName, lastName, fullName);
        User user = User.builder()
                .firstName(nameParts.firstName())
                .lastName(nameParts.lastName())
                .mobile(mobile)
                .passwordHash(null)
                .hasAccount(true)
                .setupRequired(true)
                .failedLoginAttempts(0)
                .status(UserStatus.ACTIVE)
                .role(roleRepository.findByName("ROLE_USER").orElse(null))
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
