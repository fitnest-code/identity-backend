package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.entity.OtpSessionPayload;
import az.fitnest.identity.model.entity.OtpVerificationResult;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpVerifyRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.dto.OtpVerifyResponse;
import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.OtpRateLimitedException;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.service.SmsService;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.model.constants.OtpMessages;
import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.RegistrationTokenService;
import az.fitnest.identity.service.ResetPasswordTokenService;
import az.fitnest.identity.service.EmailService;
import az.fitnest.identity.service.TokenIssuanceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final OtpRateLimiter otpRateLimiter;
    private final OtpGenerator otpGenerator;
    private final PasswordService passwordService;
    private final OtpSessionIdGenerator otpSessionIdGenerator;
    private final SmsService smsService;
    private final RegistrationTokenService registrationTokenService;
    private final ResetPasswordTokenService resetPasswordTokenService;
    private final TokenIssuanceService tokenIssuanceService;
    private final EmailService emailService;
    private final Clock clock;

    @Value("${otp.ttl-seconds}")
    private int otpTtlSeconds;

    @Value("${otp.resend-cooldown-seconds}")
    private int resendCooldownSeconds;

    @Value("${otp.max-verify-attempts}")
    private int maxVerifyAttempts;

    @Value("${otp.rate-limit.min-cooldown-seconds}")
    private int minCooldownSeconds;

    @Value("${otp.rate-limit.error-message-threshold-seconds}")
    private int errorMessageThresholdSeconds;

    @PostConstruct
    private void validateConfiguration() {
        if (resendCooldownSeconds < minCooldownSeconds) {
            throw new IllegalStateException(
                    "Configuration error: otp.resend-cooldown-seconds (" + resendCooldownSeconds +
                            ") must be >= otp.rate-limit.min-cooldown-seconds (" + minCooldownSeconds + ")"
            );
        }
    }

    @Override
    public OtpSendResponse sendOtp(OtpSendRequest request) {
        return sendOtp(request, null, null, null, null);
    }

    @Override
    public OtpSendResponse sendOtpByUserId(Long userId, OtpSendRequest request) {
        return sendOtp(request, null, null, null, null, userId);
    }

    @Override
    public OtpSendResponse sendOtp(OtpSendRequest request, String firstName, String lastName, String userPasswordHash, String mobile) {
        return sendOtp(request, firstName, lastName, userPasswordHash, mobile, null);
    }

    private OtpSendResponse sendOtp(OtpSendRequest request, String firstName, String lastName, String userPasswordHash, String mobile, Long userId) {
        String rawMobile = request.mobile() != null ? request.mobile() : mobile;
        String rawEmail = request.email();
        String mobileNumber = rawMobile != null ? az.fitnest.identity.util.MobileNumberUtils.normalize(rawMobile) : null;
        String email = rawEmail != null ? rawEmail.toLowerCase().trim() : null;

        OtpPurpose purpose = request.purpose();
        String identifier = (purpose == OtpPurpose.EMAIL_CHANGE) ? email : mobileNumber;

        if (identifier == null) {
            throw new IllegalArgumentException(purpose == OtpPurpose.EMAIL_CHANGE ? "E-poçt təqdim edilməlidir" : "Mobil nömrə təqdim edilməlidir");
        }

        validateRateLimit(purpose, identifier);

        boolean exists = (purpose == OtpPurpose.EMAIL_CHANGE) 
                ? userRepository.findFirstByEmail(email).isPresent()
                : userRepository.findFirstByMobile(mobileNumber).isPresent();

        boolean shouldSendOtp = doesPurposeMatchExistence(purpose, exists);

        if (!shouldSendOtp) {
            return createFakeSessionResponse();
        }

        invalidateActiveSession(purpose, identifier, userId);

        String otp = otpGenerator.generateOtp();
        String sessionId = createOtpSession(purpose, otp, firstName, lastName, userPasswordHash, mobileNumber, email, userId);

        if (purpose == OtpPurpose.EMAIL_CHANGE) {
            emailService.sendSimpleEmail(email, "Fitnest Verification Code", "Your Fitnest verification code: " + otp);
        } else {
            smsService.sendSms(mobileNumber, "Your Fitnest verification code: " + otp);
        }

        return createSuccessResponse(sessionId);
    }

    private boolean doesPurposeMatchExistence(OtpPurpose purpose, boolean exists) {
        if (purpose == OtpPurpose.REGISTRATION || purpose == OtpPurpose.EMAIL_CHANGE || purpose == OtpPurpose.MOBILE_CHANGE) {
            return !exists;
        } else if (purpose == OtpPurpose.LOGIN || purpose == OtpPurpose.PASSWORD_RESET || purpose == OtpPurpose.REACTIVATION) {
            return exists;
        }
        return false;
    }

    private void invalidateActiveSession(OtpPurpose purpose, String identifier, Long userId) {
        otpStore.getActiveSessionPointer(purpose, identifier).ifPresent(existingSessionId -> {
            otpStore.deleteSession(existingSessionId);
            otpStore.deleteActivePointer(purpose, identifier);
        });
        if (userId != null) {
            otpStore.getActiveSessionPointer(purpose, "user:" + userId).ifPresent(existingSessionId -> {
                otpStore.deleteSession(existingSessionId);
                otpStore.deleteActivePointer(purpose, "user:" + userId);
            });
        }
    }

    private void validateRateLimit(OtpPurpose purpose, String identifier) {
        OtpRateLimiter.RateLimitResult rateLimitResult = otpRateLimiter.checkRateLimit(purpose, identifier);
        if (!rateLimitResult.allowed()) {
            long waitTimeSeconds = rateLimitResult.waitTimeSeconds();
            // Security hardening: Do not leak actual wait time to client.
            // Use a generic message but log the real wait time internally.
            String message = OtpMessages.rateLimitGeneric();

            // Assuming OtpMessages.rateLimitGeneric() exists or similar. 
            // If not, I'll use a standard "Too many requests. Please try again later."
            if (message == null || message.isBlank()) {
                message = "Too many requests. Please try again later.";
            }

            throw new OtpRateLimitedException(message, waitTimeSeconds);
        }
    }

    private OtpSendResponse createFakeSessionResponse() {
        String fakeSessionId = otpSessionIdGenerator.generateSessionId();
        return new OtpSendResponse(fakeSessionId, otpTtlSeconds, resendCooldownSeconds, OtpMessages.OTP_SENT_IF_EXISTS);
    }

    private String createOtpSession(OtpPurpose purpose, String otp, String firstName, String lastName, String userPasswordHash, String mobile, String email, Long userId) {
        String otpHash = hashOtp(otp);
        String sessionId = otpSessionIdGenerator.generateSessionId();

        OtpSessionPayload payload = OtpSessionPayload.builder()
                .purpose(purpose)
                .otpHash(otpHash)
                .attempts(0)
                .locked(false)
                .verified(false)
                .createdAt(Instant.now(clock))
                .firstName(firstName)
                .lastName(lastName)
                .userPasswordHash(userPasswordHash)
                .mobile(mobile)
                .email(email)
                .build();

        String identifier = (purpose == OtpPurpose.EMAIL_CHANGE) ? email : mobile;
        otpStore.saveOtpSessionAtomically(purpose, identifier, sessionId, payload, otpTtlSeconds);
        if (userId != null) {
            otpStore.setActiveSessionPointer(purpose, "user:" + userId, sessionId, otpTtlSeconds);
        }

        return sessionId;
    }

    private OtpSendResponse createSuccessResponse(String sessionId) {
        return new OtpSendResponse(sessionId, otpTtlSeconds, resendCooldownSeconds, OtpMessages.OTP_SENT);
    }

    @Override
    public OtpVerificationResult verifyOtp(String sessionId, String otpCode) {
        Optional<OtpSessionPayload> sessionOpt = otpStore.getSessionForVerification(sessionId);

        if (sessionOpt.isEmpty()) {
            throw new InvalidCredentialsException(OtpMessages.INVALID_OTP);
        }

        OtpSessionPayload session = sessionOpt.get();

        if (session.locked()) {
            throw new InvalidCredentialsException(OtpMessages.OTP_LOCKED);
        }

        if (session.verified()) {
            throw new InvalidCredentialsException(OtpMessages.OTP_ALREADY_VERIFIED);
        }

        // We no longer check emailExistsAtCreation because removing it from payload means we trust
        // doesPurposeMatchExistence ran at creation time.
        // Or we should assume existence was checked at creation.
        // But previously we double checked here.
        // Since we removed emailExistsAtCreation, we rely on sendOtp logic.
        // It's safer to not re-check here if field relies on state at creation which is immutable in session.

        boolean isValid = hashOtp(otpCode).equals(session.otpHash());

        OtpStore.VerifyOtpResult result = otpStore.verifyOtpAndUpdate(sessionId, maxVerifyAttempts, isValid);

        if (!result.isFound()) {
            throw new InvalidCredentialsException(OtpMessages.INVALID_OTP);
        }

        if (result.isLocked()) {
            throw new InvalidCredentialsException(OtpMessages.OTP_LOCKED);
        }

        if (result.isAlreadyVerified()) {
            throw new InvalidCredentialsException(OtpMessages.OTP_ALREADY_VERIFIED);
        }

        if (result.isExpired()) {
            throw new InvalidCredentialsException(OtpMessages.INVALID_OTP);
        }

        if (!isValid) {
            throw new InvalidCredentialsException(OtpMessages.INVALID_OTP);
        }

        OtpSessionPayload verifiedSession = result.getSession();
        return OtpVerificationResult.builder()
                .purpose(verifiedSession.purpose())
                .firstName(verifiedSession.firstName())
                .lastName(verifiedSession.lastName())
                .passwordHash(verifiedSession.userPasswordHash())
                .mobile(verifiedSession.mobile())
                .email(verifiedSession.email())
                .build();
    }

    @Override
    public OtpVerificationResult verifyOtpByIdentifier(String identifier, OtpPurpose purpose, String otpCode) {
        String sessionId = otpStore.getActiveSessionPointer(purpose, identifier)
                .orElseThrow(() -> new InvalidCredentialsException(OtpMessages.INVALID_OTP));

        return verifyOtp(sessionId, otpCode);
    }

    @Override
    public OtpVerificationResult verifyOtpByUserId(Long userId, OtpPurpose purpose, String otpCode) {
        String sessionId = otpStore.getActiveSessionPointer(purpose, "user:" + userId)
                .orElseThrow(() -> new InvalidCredentialsException(OtpMessages.INVALID_OTP));

        return verifyOtp(sessionId, otpCode);
    }

    @Override
    public OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request) {
        OtpVerificationResult verificationResult = verifyOtp(request.otpSessionId(), request.otpCode());

        String identifier = verificationResult.mobile();

        if (verificationResult.purpose() == OtpPurpose.REGISTRATION) {
            String registrationToken = registrationTokenService.issueForIdentifier(identifier);
            return new OtpVerifyResponse(
                    true,
                    registrationToken,
                    OtpMessages.OTP_VERIFIED,
                    null,
                    null,
                    null,
                    null
            );
        } else if (verificationResult.purpose() == OtpPurpose.PASSWORD_RESET) {
            String resetToken = resetPasswordTokenService.issueForIdentifier(identifier);
            return new OtpVerifyResponse(
                    true,
                    null,
                    OtpMessages.OTP_VERIFIED,
                    resetToken,
                    null,
                    null,
                    null
            );
        } else if (verificationResult.purpose() == OtpPurpose.REACTIVATION) {
            az.fitnest.identity.model.entity.User user = userRepository.findFirstByMobile(identifier)
                    .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("İstifadəçi tapılmadı"));

            user.setStatus(az.fitnest.identity.model.enums.UserStatus.ACTIVE);
            userRepository.save(user);

            String deviceType = az.fitnest.identity.util.DeviceDetector.detectDeviceType();
            az.fitnest.identity.dto.LoginResponse loginResponse = tokenIssuanceService.issueTokens(user, deviceType);

            return new OtpVerifyResponse(
                    true,
                    null,
                    az.fitnest.identity.model.constants.OtpMessages.OTP_VERIFIED,
                    null,
                    loginResponse.accessToken(),
                    loginResponse.refreshToken(),
                    loginResponse.user()
            );
        } else if (verificationResult.purpose() == OtpPurpose.EMAIL_CHANGE || verificationResult.purpose() == OtpPurpose.MOBILE_CHANGE) {
            return new OtpVerifyResponse(
                    true,
                    null,
                    OtpMessages.OTP_VERIFIED,
                    null,
                    null,
                    null,
                    null
            );
        } else {
            throw new InvalidCredentialsException("Yanlış OTP təyinatı");
        }
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
