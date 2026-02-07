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

    private final EmailNormalizationService emailNormalizationService;
    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final OtpRateLimiter otpRateLimiter;
    private final OtpGenerator otpGenerator;
    private final PasswordService passwordService;
    private final OtpSessionIdGenerator otpSessionIdGenerator;
    private final EmailService emailService;
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

    private boolean doesPurposeMatchEmailExistence(OtpPurpose purpose, String email) {
        boolean emailExists = userRepository.existsByEmailIgnoreCase(email);
        if (purpose == OtpPurpose.REGISTRATION) {
            return !emailExists;
        } else if (purpose == OtpPurpose.LOGIN || purpose == OtpPurpose.PASSWORD_RESET) {
            return emailExists;
        }
        return false;
    }

    private boolean doesPurposeMatchEmailExistence(OtpPurpose purpose, boolean emailExistsAtCreation) {
        if (purpose == OtpPurpose.REGISTRATION) {
            return !emailExistsAtCreation;
        } else if (purpose == OtpPurpose.LOGIN || purpose == OtpPurpose.PASSWORD_RESET) {
            return emailExistsAtCreation;
        }
        return false;
    }

    public OtpSendResponse sendOtp(OtpSendRequest request) {
        return sendOtp(request, null, null, null, null);
    }

    public OtpSendResponse sendOtp(OtpSendRequest request, String firstName, String lastName, String userPasswordHash, String mobile) {
        String email = emailNormalizationService.normalize(request.getEmail());
        OtpPurpose purpose = request.getPurpose();

        validateRateLimit(purpose, email);

        boolean emailExists = userRepository.existsByEmailIgnoreCase(email);
        boolean shouldSendEmail = doesPurposeMatchEmailExistence(purpose, email);

        if (!shouldSendEmail) {
            return createFakeSessionResponse();
        }

        invalidateActiveSession(purpose, email);

        String otp = otpGenerator.generateOtp();
        String sessionId = createOtpSession(email, purpose, otp, emailExists, firstName, lastName, userPasswordHash, mobile);

        emailService.sendHtmlEmail(email, "Your Fitnest verification code", "otp", java.util.Map.of("otp", otp));

        return createSuccessResponse(sessionId);
    }

    private void invalidateActiveSession(OtpPurpose purpose, String email) {
        otpStore.getActiveSessionPointer(purpose, email).ifPresent(existingSessionId -> {
            otpStore.deleteSession(existingSessionId);
            otpStore.deleteActivePointer(purpose, email);
        });
    }

    private void validateRateLimit(OtpPurpose purpose, String email) {
        OtpRateLimiter.RateLimitResult rateLimitResult = otpRateLimiter.checkRateLimit(purpose, email);
        if (!rateLimitResult.isAllowed()) {
            long waitTimeSeconds = rateLimitResult.getWaitTimeSeconds();
            String message = waitTimeSeconds <= errorMessageThresholdSeconds
                    ? OtpMessages.rateLimitSeconds(waitTimeSeconds)
                    : OtpMessages.rateLimitMinutes(waitTimeSeconds / 60);
            throw new OtpRateLimitedException(message);
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

    private String createOtpSession(String email, OtpPurpose purpose, String otp, boolean emailExists, String firstName, String lastName, String userPasswordHash, String mobile) {
        String otpHash = hashOtp(otp);
        String sessionId = otpSessionIdGenerator.generateSessionId();

        OtpSessionPayload payload = OtpSessionPayload.builder()
                .email(email)
                .purpose(purpose)
                .otpHash(otpHash)
                .attempts(0)
                .locked(false)
                .verified(false)
                .createdAt(Instant.now(clock))
                .emailExistsAtCreation(emailExists)
                .firstName(firstName)
                .lastName(lastName)
                .userPasswordHash(userPasswordHash)
                .mobile(mobile)
                .build();

        otpStore.saveOtpSessionAtomically(purpose, email, sessionId, payload, otpTtlSeconds);

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

        if (session.getEmailExistsAtCreation() == null) {
            throw new InvalidCredentialsException(OtpMessages.INVALID_OTP);
        }

        boolean purposeMatches = doesPurposeMatchEmailExistence(
                session.getPurpose(), 
                session.getEmailExistsAtCreation()
        );

        if (!purposeMatches) {
            throw new InvalidCredentialsException(OtpMessages.INVALID_OTP);
        }

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
                .email(verifiedSession.getEmail())
                .purpose(verifiedSession.getPurpose())
                .firstName(verifiedSession.getFirstName())
                .lastName(verifiedSession.getLastName())
                .passwordHash(verifiedSession.getUserPasswordHash())
                .mobile(verifiedSession.getMobile())
                .build();
    }

    public OtpVerificationResult verifyOtpByEmail(String email, OtpPurpose purpose, String otpCode) {
        String normalizedEmail = emailNormalizationService.normalize(email);
        String sessionId = otpStore.getActiveSessionPointer(purpose, normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException(OtpMessages.INVALID_OTP));
        
        return verifyOtp(sessionId, otpCode);
    }

    public OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request) {
        OtpVerificationResult verificationResult = verifyOtp(request.getOtpSessionId(), request.getOtpCode());
        
        if (verificationResult.getPurpose() != OtpPurpose.REGISTRATION) {
            throw new InvalidCredentialsException("Invalid OTP purpose for registration token");
        }
        
        String registrationToken = registrationTokenService.issueForEmail(verificationResult.getEmail());
        
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
        
        return resetPasswordTokenService.issueForEmail(verificationResult.getEmail());
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
