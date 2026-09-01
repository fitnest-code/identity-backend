package az.fitnest.identity.service.impl;

import az.fitnest.identity.dto.request.AppleSocialRequest;
import az.fitnest.identity.dto.request.GoogleSocialRequest;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.ForbiddenException;
import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.model.entity.SocialAuth;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.enums.SocialProvider;
import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.SocialAuthRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.AppleTokenVerifier;
import az.fitnest.identity.service.GoogleTokenVerifier;
import az.fitnest.identity.service.LegalService;
import az.fitnest.identity.service.SocialAuthService;
import az.fitnest.identity.service.TokenIssuanceService;
import az.fitnest.identity.service.UserProfileGrpcClient;
import az.fitnest.identity.service.DeviceService;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.service.WelcomeBonusService;
import az.fitnest.identity.repository.UserConsentRepository;
import az.fitnest.identity.model.entity.UserConsent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final RedisTokenService redisTokenService;
    private final AuthTokenRepository authTokenRepository;
    private final DeviceService deviceService;
    private final az.fitnest.identity.service.OtpService otpService;
    private final UserService userService;
    private final UserConsentRepository userConsentRepository;
    private final WelcomeBonusService welcomeBonusService;

    @Autowired
    @Lazy
    private SocialAuthServiceImpl self;

    @Override
    @Transactional
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
                claims.picture(),
                null,
                null
        );
    }

    @Override
    @Transactional
    public LoginResponse socialLoginGoogleV2(az.fitnest.identity.dto.request.GoogleSocialRequestV2 request) {
        log.info("Starting Google V2 social login process");
        GoogleTokenVerifier.GoogleTokenClaims claims = googleTokenVerifier.verify(request.idToken());
        log.info("Google token verified successfully for email: {}", claims.email());

        return processSocialLogin(
                SocialProvider.GOOGLE,
                claims.userId(),
                claims.email(),
                claims.givenName(),
                claims.familyName(),
                claims.name(),
                claims.picture(),
                request.deviceId(),
                request.deviceType()
        );
    }

    @Override
    @Transactional
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
                null,
                null,
                null
        );
    }

    @Override
    @Transactional
    public LoginResponse socialLoginAppleV2(az.fitnest.identity.dto.request.AppleSocialRequestV2 request) {
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
                null,
                request.deviceId(),
                request.deviceType()
        );
    }

    private String cleanPreviousSessionAndGetDeviceType(Long userId, String deviceType) {
        String activeJti = redisTokenService.getActiveSession(userId, deviceType);
        if (activeJti != null) {
            redisTokenService.revokeAccessToken(activeJti);
            authTokenRepository.deleteByJti(activeJti);
        }
        return deviceType;
    }

    private LoginResponse processSocialLogin(SocialProvider provider, String providerId, String email,
                                             String firstName, String lastName, String fullName, String pictureUrl,
                                             String deviceId, String deviceType) {
        log.info("Processing social login for provider: {}, providerId: {}, email: {}", provider, providerId, email);

        Optional<SocialAuth> existingSocialAuth = socialAuthRepository.findByProviderAndProviderId(provider, providerId);

        if (existingSocialAuth.isPresent()) {
            SocialAuth socialAuth = existingSocialAuth.get();
            log.info("Found existing social auth record for user ID: {}", socialAuth.getUserId());
            Optional<User> userOpt = userRepository.findById(socialAuth.getUserId());
            if (userOpt.isPresent()) {
                User user = self.findAndReactivateUser(socialAuth.getUserId());

                user = deviceService.validateAndBindDeviceForLogin(user, deviceId, deviceType, false);

                var profile = userProfileGrpcClient.getUserProfileDetails(user.getId());
                if (profile == null || profile.getProfileImageUrl() == null || profile.getProfileImageUrl().isBlank()) {
                    log.info("Updating profile image for existing user: {}", user.getId());
                    userProfileGrpcClient.updateProfileImage(user.getId(), pictureUrl);
                }
                log.info("Issuing tokens for existing user: {}", user.getId());
                return tokenIssuanceService.issueTokens(user, cleanPreviousSessionAndGetDeviceType(user.getId(), deviceType));
            } else {
                log.warn("Orphaned SocialAuth record found for user ID: {}, deleting stale social auth", socialAuth.getUserId());
                socialAuthRepository.delete(socialAuth);
            }
        }

        if (email != null && !email.isEmpty()) {
            log.info("No social auth record found, checking if user exists by email in user-backend: {}", email);
            var userByEmail = userProfileGrpcClient.getUserByEmail(email);
            if (userByEmail != null) {
                Long userId = userByEmail.userId();
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    log.info("Found existing user by email in user-backend: {}. UserId: {}. Linking {} account.", email, userId, provider);
                    User user = self.findAndReactivateUser(userId);

                    user = deviceService.validateAndBindDeviceForLogin(user, deviceId, deviceType, false);

                    var profile = userProfileGrpcClient.getUserProfileDetails(user.getId());
                    if (profile == null || profile.getProfileImageUrl() == null || profile.getProfileImageUrl().isBlank()) {
                        log.info("Updating profile image for existing user (by email): {}", user.getId());
                        userProfileGrpcClient.updateProfileImage(user.getId(), pictureUrl);
                    }
                    self.linkSocialAccount(user.getId(), provider, providerId);
                    return tokenIssuanceService.issueTokens(user, cleanPreviousSessionAndGetDeviceType(user.getId(), deviceType));
                } else {
                    log.warn("User backend returned user by email {} with userId {}, but user does not exist in identity-backend.", email, userId);
                }
            }
        }

        log.info("No existing user found. Creating new user for social login.");
        User newUser = self.createIdentityUser(deviceId);
        NameParts nameParts = resolveNameParts(firstName, lastName, fullName);
        log.info("Creating user profile in user-backend for user ID: {}", newUser.getId());
        userProfileGrpcClient.createUserProfile(newUser.getId(), nameParts.firstName(), nameParts.lastName(), email);

        if (pictureUrl != null && !pictureUrl.isBlank()) {
            userProfileGrpcClient.updateProfileImage(newUser.getId(), pictureUrl);
        }
        self.linkSocialAccount(newUser.getId(), provider, providerId);

        log.info("Issuing tokens for new user: {}", newUser.getId());
        legalService.autoAcceptLatestConsents(newUser.getId());
        welcomeBonusService.tryPublishWelcomeBonusEligible(newUser);
        return tokenIssuanceService.issueTokens(newUser, cleanPreviousSessionAndGetDeviceType(newUser.getId(), deviceType));
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
    public User createIdentityUser(String deviceId) {
        User user = User.builder()
                .mobile(null)
                .passwordHash(null)
                .hasAccount(true)
                .setupRequired(true)
                .failedLoginAttempts(0)
                .status(UserStatus.PENDING_REGISTRATION)
                .hasLocalPassword(false)
                .deviceId(deviceId != null ? deviceId.trim() : null)
                .role(roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new IllegalStateException("System error: Default role ROLE_USER not found")))
                .build();

        User savedUser = userRepository.save(user);
        if (deviceId != null && !deviceId.isBlank()) {
            deviceService.registerDevice(savedUser, deviceId);
        }
        return savedUser;
    }

    private void handleUserStatus(User user) {
        switch (user.getStatus()) {
            case ACTIVE, PENDING_REGISTRATION -> {
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

    @Override
    @Transactional
    public az.fitnest.identity.dto.response.OtpSendResponse requestAddNumberOtpGoogle(Long userId, az.fitnest.identity.dto.request.AddNumberOtpRequest request) {
        return requestAddNumberOtp(userId, request, SocialProvider.GOOGLE);
    }

    @Override
    @Transactional
    public az.fitnest.identity.dto.response.OtpSendResponse requestAddNumberOtpApple(Long userId, az.fitnest.identity.dto.request.AddNumberOtpRequest request) {
        return requestAddNumberOtp(userId, request, SocialProvider.APPLE);
    }

    private az.fitnest.identity.dto.response.OtpSendResponse requestAddNumberOtp(Long userId, az.fitnest.identity.dto.request.AddNumberOtpRequest request, SocialProvider provider) {
        if (userId == null) {
            throw new az.fitnest.identity.exception.UnauthorizedException("error.auth.unauthorized");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("error.auth.user_not_found"));
        
        if (user.getMobile() != null && !user.getMobile().trim().isEmpty()) {
            throw new az.fitnest.identity.exception.BadRequestException("error.service.invalid_operation_context");
        }

        String normalizedMobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.mobile());
        if (normalizedMobile == null || normalizedMobile.isBlank()) {
            throw new az.fitnest.identity.exception.ValidationException("error.validation", "INVALID_MOBILE");
        }

        Optional<User> existingUserOpt = userRepository.findFirstByMobile(normalizedMobile);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            String existingEmail = null;
            try {
                var profile = userProfileGrpcClient.getUserProfileDetails(existingUser.getId());
                if (profile != null) {
                    existingEmail = profile.getEmail();
                }
            } catch (Exception ignored) {}

            if (existingEmail != null && !existingEmail.trim().isEmpty()) {
                throw new az.fitnest.identity.exception.ConflictException("error.service.mobile_already_in_use");
            }
        }

        // We fetch profile image or email if needed by flow
        String email = null;
        try {
            var profile = userProfileGrpcClient.getUserProfileDetails(userId);
            if (profile != null) {
                email = profile.getEmail();
            }
        } catch (Exception ignored) {}

        az.fitnest.identity.dto.request.OtpSendRequest otpSendRequest = az.fitnest.identity.dto.request.OtpSendRequest.builder()
                .purpose(provider == SocialProvider.GOOGLE ? az.fitnest.identity.model.enums.OtpPurpose.ADD_NUMBER_GOOGLE : az.fitnest.identity.model.enums.OtpPurpose.ADD_NUMBER_APPLE)
                .mobile(normalizedMobile)
                .email(email)
                .build();
        return otpService.sendOtpByUserId(user.getId(), otpSendRequest);
    }

    @Override
    @Transactional
    public LoginResponse verifyAddNumberOtpGoogle(Long userId, az.fitnest.identity.dto.request.AddNumberOtpVerifyRequest request) {
        return verifyAddNumberOtp(userId, request, az.fitnest.identity.model.enums.OtpPurpose.ADD_NUMBER_GOOGLE);
    }

    @Override
    @Transactional
    public LoginResponse verifyAddNumberOtpApple(Long userId, az.fitnest.identity.dto.request.AddNumberOtpVerifyRequest request) {
        return verifyAddNumberOtp(userId, request, az.fitnest.identity.model.enums.OtpPurpose.ADD_NUMBER_APPLE);
    }

    private LoginResponse verifyAddNumberOtp(Long userId, az.fitnest.identity.dto.request.AddNumberOtpVerifyRequest request, az.fitnest.identity.model.enums.OtpPurpose purpose) {
        if (userId == null) {
            throw new az.fitnest.identity.exception.UnauthorizedException("error.auth.unauthorized");
        }
        var verificationResult = otpService.verifyOtp(request.otpSessionId(), request.otpCode());
        if (verificationResult.purpose() != purpose) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException("error.service.invalid_operation_context");
        }

        if (!userId.equals(verificationResult.userId())) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException("error.service.invalid_operation_context");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("error.auth.user_not_found"));

        if (user.getMobile() != null && !user.getMobile().trim().isEmpty()) {
            throw new az.fitnest.identity.exception.BadRequestException("error.service.invalid_operation_context");
        }

        String normalizedMobile = verificationResult.mobile();
        Optional<User> existingUserOpt = userRepository.findFirstByMobile(normalizedMobile);
        User finalUser;
        if (existingUserOpt.isPresent()) {
            User existingUser = self.findAndReactivateUser(existingUserOpt.get().getId());
            String existingEmail = null;
            try {
                var profile = userProfileGrpcClient.getUserProfileDetails(existingUser.getId());
                if (profile != null) {
                    existingEmail = profile.getEmail();
                }
            } catch (Exception ignored) {}

            if (existingEmail != null && !existingEmail.trim().isEmpty()) {
                throw new az.fitnest.identity.exception.ConflictException("error.service.mobile_already_in_use");
            }

            // Merge details
            String socialEmail = null;
            String socialFirstName = null;
            String socialLastName = null;
            String socialProfileImageUrl = null;
            try {
                var profile = userProfileGrpcClient.getUserProfileDetails(userId);
                if (profile != null) {
                    socialEmail = profile.getEmail();
                    socialFirstName = profile.getFirstName();
                    socialLastName = profile.getLastName();
                    socialProfileImageUrl = profile.getProfileImageUrl();
                }
            } catch (Exception ignored) {}

            // Update SocialAuth records
            List<SocialAuth> socialAuths = socialAuthRepository.findByUserId(userId);
            for (SocialAuth sa : socialAuths) {
                sa.setUserId(existingUser.getId());
                socialAuthRepository.save(sa);
            }

            // Update user profile in user-backend
            userProfileGrpcClient.createUserProfile(existingUser.getId(), socialFirstName, socialLastName, socialEmail);
            if (socialProfileImageUrl != null && !socialProfileImageUrl.isBlank()) {
                userProfileGrpcClient.updateProfileImage(existingUser.getId(), socialProfileImageUrl);
            }

            // Auto accept latest consents for existingUser
            legalService.autoAcceptLatestConsents(existingUser.getId());

            // Delete consents of socialUser
            List<UserConsent> consents = userConsentRepository.findAllByUserId(userId);
            if (!consents.isEmpty()) {
                userConsentRepository.deleteAll(consents);
            }

            // Hard delete the temporary social user
            userService.hardDeleteUser(userId);

            finalUser = existingUser;
        } else {
            user.setMobile(normalizedMobile);
            if (user.getStatus() == UserStatus.PENDING_REGISTRATION) {
                user.setStatus(UserStatus.ACTIVE);
            }
            user = userRepository.save(user);
            finalUser = user;
        }

        finalUser = deviceService.validateAndBindDeviceForLogin(finalUser, request.deviceId(), request.deviceType(), false);

        welcomeBonusService.tryPublishWelcomeBonusEligible(finalUser);

        return tokenIssuanceService.issueTokens(finalUser, cleanPreviousSessionAndGetDeviceType(finalUser.getId(), request.deviceType()));
    }

    private record NameParts(String firstName, String lastName) {
    }
}
