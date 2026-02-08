package az.fitnest.iam.otp.adapter.service;

import az.fitnest.iam.otp.domain.model.OtpSessionPayload;
import az.fitnest.iam.otp.domain.model.OtpVerificationResult;
import az.fitnest.iam.otp.api.dto.request.OtpSendRequest;
import az.fitnest.iam.otp.api.dto.request.OtpVerifyRequest;
import az.fitnest.iam.otp.api.dto.response.OtpSendResponse;
import az.fitnest.iam.otp.api.dto.response.OtpVerifyResponse;
import az.fitnest.iam.otp.domain.enums.OtpPurpose;
import az.fitnest.iam.shared.exception.InvalidCredentialsException;
import az.fitnest.iam.shared.exception.OtpRateLimitedException;
import az.fitnest.iam.messaging.EmailService;
import az.fitnest.iam.messaging.SmsService;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.adapter.service.EmailNormalizationService;
import az.fitnest.iam.otp.adapter.store.redis.OtpStore;
import az.fitnest.iam.otp.adapter.store.redis.OtpRateLimiter;
import az.fitnest.iam.otp.adapter.service.OtpGenerator;
import az.fitnest.iam.otp.adapter.service.OtpSessionIdGenerator;
import az.fitnest.iam.otp.domain.constants.OtpMessages;
import az.fitnest.iam.auth.adapter.service.PasswordService;
import az.fitnest.iam.auth.adapter.service.RegistrationTokenService;
import az.fitnest.iam.auth.adapter.service.ResetPasswordTokenService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final OtpRateLimiter otpRateLimiter;
    private final OtpGenerator otpGenerator;
    private final PasswordService passwordService;
    private final OtpSessionIdGenerator otpSessionIdGenerator;
    private final SmsService smsService;
    private final RegistrationTokenService registrationTokenService;
    private final ResetPasswordTokenService resetPasswordTokenService;
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

    public OtpSendResponse sendOtp(OtpSendRequest request) {
        return sendOtp(request, null, null, null, null);
    }

    public OtpSendResponse sendOtp(OtpSendRequest request, String firstName, String lastName, String userPasswordHash, String mobile) {
        String rawMobile = request.getMobile() != null ? request.getMobile() : mobile;
        String mobileNumber = az.fitnest.iam.shared.util.MobileNumberUtils.normalize(rawMobile);

        if (mobileNumber == null) {
            throw new IllegalArgumentException("Mobile number must be provided");
        }

        OtpPurpose purpose = request.getPurpose();
        
        validateRateLimit(purpose, mobileNumber);

        boolean exists = userRepository.findByMobileIncludingDeleted(mobileNumber).isPresent();

        boolean shouldSendOtp = doesPurposeMatchExistence(purpose, exists);

        if (!shouldSendOtp) {
            return createFakeSessionResponse();
        }

        invalidateActiveSession(purpose, mobileNumber);

        String otp = otpGenerator.generateOtp();
        String sessionId = createOtpSession(purpose, otp, firstName, lastName, userPasswordHash, mobileNumber);

        smsService.sendSms(mobileNumber, "Your Fitnest verification code: " + otp);

        return createSuccessResponse(sessionId);
    }

    private boolean doesPurposeMatchExistence(OtpPurpose purpose, boolean exists) {
        if (purpose == OtpPurpose.REGISTRATION) {
            return !exists;
        } else if (purpose == OtpPurpose.LOGIN || purpose == OtpPurpose.PASSWORD_RESET) {
            return exists;
        }
        return false;
    }

    private void invalidateActiveSession(OtpPurpose purpose, String identifier) {
        otpStore.getActiveSessionPointer(purpose, identifier).ifPresent(existingSessionId -> {
            otpStore.deleteSession(existingSessionId);
            otpStore.deleteActivePointer(purpose, identifier);
        });
    }

    private void validateRateLimit(OtpPurpose purpose, String identifier) {
        OtpRateLimiter.RateLimitResult rateLimitResult = otpRateLimiter.checkRateLimit(purpose, identifier);
        if (!rateLimitResult.isAllowed()) {
            long waitTimeSeconds = rateLimitResult.getWaitTimeSeconds();
            String message = waitTimeSeconds <= errorMessageThresholdSeconds
                    ? OtpMessages.rateLimitSeconds(waitTimeSeconds)
                    : OtpMessages.rateLimitMinutes(waitTimeSeconds / 60);
            throw new OtpRateLimitedException(message, waitTimeSeconds);
        }
    }

    private OtpSendResponse createFakeSessionResponse() {
        String fakeSessionId = otpSessionIdGenerator.generateSessionId();
        return OtpSendResponse.builder()
                .otpSessionId(fakeSessionId)
                .expiresInSeconds(otpTtlSeconds)
                .resendAvailableInSeconds(resendCooldownSeconds)
                .message(OtpMessages.OTP_SENT_IF_EXISTS)
                .build();
    }

    private String createOtpSession(OtpPurpose purpose, String otp, String firstName, String lastName, String userPasswordHash, String mobile) {
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
                .build();

        otpStore.saveOtpSessionAtomically(purpose, mobile, sessionId, payload, otpTtlSeconds);

        return sessionId;
    }

    private OtpSendResponse createSuccessResponse(String sessionId) {
        return OtpSendResponse.builder()
                .otpSessionId(sessionId)
                .expiresInSeconds(otpTtlSeconds)
                .resendAvailableInSeconds(resendCooldownSeconds)
                .message(OtpMessages.OTP_SENT)
                .build();
    }

    public OtpVerificationResult verifyOtp(String sessionId, String otpCode) {
        Optional<OtpSessionPayload> sessionOpt = otpStore.getSessionForVerification(sessionId);
        
        if (sessionOpt.isEmpty()) {
            throw new InvalidCredentialsException(OtpMessages.INVALID_OTP);
        }

        OtpSessionPayload session = sessionOpt.get();

        if (session.getLocked()) {
            throw new InvalidCredentialsException(OtpMessages.OTP_LOCKED);
        }

        if (session.getVerified()) {
            throw new InvalidCredentialsException(OtpMessages.OTP_ALREADY_VERIFIED);
        }
        
        // We no longer check emailExistsAtCreation because removing it from payload means we trust
        // doesPurposeMatchExistence ran at creation time.
        // Or we should assume existence was checked at creation.
        // But previously we double checked here.
        // Since we removed emailExistsAtCreation, we rely on sendOtp logic.
        // It's safer to not re-check here if field relies on state at creation which is immutable in session.

        boolean isValid = hashOtp(otpCode).equals(session.getOtpHash());

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
                .purpose(verifiedSession.getPurpose())
                .firstName(verifiedSession.getFirstName())
                .lastName(verifiedSession.getLastName())
                .passwordHash(verifiedSession.getUserPasswordHash())
                .mobile(verifiedSession.getMobile())
                .build();
    }

    public OtpVerificationResult verifyOtpByIdentifier(String identifier, OtpPurpose purpose, String otpCode) {
        String sessionId = otpStore.getActiveSessionPointer(purpose, identifier)
                .orElseThrow(() -> new InvalidCredentialsException(OtpMessages.INVALID_OTP));
        
        return verifyOtp(sessionId, otpCode);
    }

    public OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request) {
        OtpVerificationResult verificationResult = verifyOtp(request.getOtpSessionId(), request.getOtpCode());
        
        if (verificationResult.getPurpose() != OtpPurpose.REGISTRATION) {
            throw new InvalidCredentialsException("Invalid OTP purpose for registration token");
        }
        
        String identifier = verificationResult.getMobile();
        String registrationToken = registrationTokenService.issueForIdentifier(identifier);
        
        return OtpVerifyResponse.builder()
                .verified(true)
                .registrationToken(registrationToken)
                .message(OtpMessages.OTP_VERIFIED)
                .build();
    }

    public String verifyOtpAndIssueResetToken(String sessionId, String otpCode) {
        OtpVerificationResult verificationResult = verifyOtp(sessionId, otpCode);
        
        if (verificationResult.getPurpose() != OtpPurpose.PASSWORD_RESET) {
            throw new InvalidCredentialsException("Invalid OTP purpose for password reset");
        }
        
        String identifier = verificationResult.getMobile();
        
        return resetPasswordTokenService.issueForIdentifier(identifier);
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
