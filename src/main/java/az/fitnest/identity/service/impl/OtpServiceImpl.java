package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.entity.OtpSessionPayload;
import az.fitnest.identity.model.entity.OtpVerificationResult;
import az.fitnest.identity.dto.request.OtpSendRequest;
import az.fitnest.identity.dto.request.OtpVerifyRequest;
import az.fitnest.identity.dto.response.OtpSendResponse;
import az.fitnest.identity.dto.response.OtpVerifyResponse;
import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.OtpRateLimitedException;
import az.fitnest.identity.exception.OtpVerificationException;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.service.SmsService;
import az.fitnest.identity.repository.UserRepository;

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
import az.fitnest.identity.mapper.OtpSendResponseMapper;
import az.fitnest.identity.mapper.OtpVerifyResponseMapper;

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
    private final org.springframework.context.MessageSource messageSource;
    private final OtpSendResponseMapper otpSendResponseMapper;
    private final OtpVerifyResponseMapper otpVerifyResponseMapper;

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
        String rawMobile = request.getMobile() != null ? request.getMobile() : mobile;
        String rawEmail = request.getEmail();
        String mobileNumber = rawMobile != null ? az.fitnest.identity.util.MobileNumberUtils.normalize(rawMobile) : null;
        String email = rawEmail != null ? rawEmail.toLowerCase().trim() : null;

        OtpPurpose purpose = request.getPurpose();
        String identifier = (purpose == OtpPurpose.EMAIL_CHANGE) ? email : mobileNumber;

        if (identifier == null) {
            throw new IllegalArgumentException(purpose == OtpPurpose.EMAIL_CHANGE ? getMessage("error.service.missing_email") : getMessage("error.service.missing_mobile"));
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

        String otp = otpGenerator.generateOtp(purpose);
        String sessionId = createOtpSession(purpose, otp, firstName, lastName, userPasswordHash, mobileNumber, email, userId);

        if (purpose == OtpPurpose.EMAIL_CHANGE) {
            emailService.sendSimpleEmail(email, "Fitnest Verification Code", "Your Fitnest verification code: " + otp);
        } else {
            smsService.sendSms(mobileNumber, "Your Fitnest verification code: " + otp);
        }

        return otpSendResponseMapper.toResponse(sessionId, otpTtlSeconds, resendCooldownSeconds, getMessage("success.otp.sent"));
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
            String message = getMessage("error.otp.rate_limit_generic");

            throw new OtpRateLimitedException(message, waitTimeSeconds);
        }
    }

    private OtpSendResponse createFakeSessionResponse() {
        String fakeSessionId = otpSessionIdGenerator.generateSessionId();
        return new OtpSendResponse(fakeSessionId, otpTtlSeconds, resendCooldownSeconds, getMessage("success.otp.sent_if_exists"));
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

    @Override
    public OtpVerificationResult verifyOtp(String sessionId, String otpCode) {
        Optional<OtpSessionPayload> sessionOpt = otpStore.getSessionForVerification(sessionId);

        if (sessionOpt.isEmpty()) {
            throw new OtpVerificationException("error.otp.invalid");
        }

        OtpSessionPayload session = sessionOpt.get();

        if (session.locked()) {
            throw new OtpVerificationException("error.otp.locked");
        }

        if (session.verified()) {
            throw new OtpVerificationException("error.otp.already_verified");
        }

        boolean isValid = hashOtp(otpCode).equals(session.otpHash());

        OtpStore.VerifyOtpResult result = otpStore.verifyOtpAndUpdate(sessionId, maxVerifyAttempts, isValid);

        if (!result.isFound()) {
            throw new OtpVerificationException("error.otp.invalid");
        }

        if (result.isLocked()) {
            throw new OtpVerificationException("error.otp.locked");
        }

        if (result.isAlreadyVerified()) {
            throw new OtpVerificationException("error.otp.already_verified");
        }

        if (result.isExpired()) {
            throw new OtpVerificationException("error.otp.invalid");
        }

        if (!isValid) {
            throw new OtpVerificationException("error.otp.invalid");
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
                .orElseThrow(() -> new OtpVerificationException("error.otp.invalid"));

        return verifyOtp(sessionId, otpCode);
    }

    @Override
    public OtpVerificationResult verifyOtpByUserId(Long userId, OtpPurpose purpose, String otpCode) {
        String sessionId = otpStore.getActiveSessionPointer(purpose, "user:" + userId)
                .orElseThrow(() -> new OtpVerificationException("error.otp.invalid"));

        return verifyOtp(sessionId, otpCode);
    }

    @Override
    public OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request) {
        OtpVerificationResult verificationResult = verifyOtp(request.otpSessionId(), request.otpCode());

        String identifier = verificationResult.mobile();

        if (verificationResult.purpose() == OtpPurpose.REGISTRATION) {
            String registrationToken = registrationTokenService.issueForIdentifier(identifier);
            return otpVerifyResponseMapper.toResponse(
                    true,
                    registrationToken,
                    getMessage("success.otp.verified"),
                    null,
                    null,
                    null,
                    null
            );
        } else if (verificationResult.purpose() == OtpPurpose.PASSWORD_RESET) {
            String resetToken = resetPasswordTokenService.issueForIdentifier(identifier);
            return otpVerifyResponseMapper.toResponse(
                    true,
                    null,
                    getMessage("success.otp.verified"),
                    resetToken,
                    null,
                    null,
                    null
            );
        } else if (verificationResult.purpose() == OtpPurpose.REACTIVATION) {
            az.fitnest.identity.model.entity.User user = userRepository.findFirstByMobile(identifier)
                    .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("error.resource.not_found"));

            user.setStatus(az.fitnest.identity.model.enums.UserStatus.ACTIVE);
            user.setInactiveAt(null);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);

            String deviceType = az.fitnest.identity.util.DeviceDetector.detectDeviceType();
            az.fitnest.identity.dto.response.LoginResponse loginResponse = tokenIssuanceService.issueTokens(user, deviceType);

            return otpVerifyResponseMapper.toResponse(
                    true,
                    null,
                    getMessage("success.otp.verified"),
                    null,
                    loginResponse.accessToken(),
                    loginResponse.refreshToken(),
                    loginResponse.user()
            );
        } else if (verificationResult.purpose() == OtpPurpose.EMAIL_CHANGE || verificationResult.purpose() == OtpPurpose.MOBILE_CHANGE) {
            return otpVerifyResponseMapper.toResponse(
                    true,
                    null,
                    getMessage("success.otp.verified"),
                    null,
                    null,
                    null,
                    null
            );
        } else {
            throw new InvalidCredentialsException("error.service.invalid_operation_context");
        }
    }

    @Override
    public OtpSendResponse resendOtp(String sessionId, OtpPurpose purpose) {
        Optional<OtpSessionPayload> sessionOpt = otpStore.getSessionForVerification(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new OtpVerificationException("error.otp.invalid");
        }
        OtpSessionPayload session = sessionOpt.get();
        long ttlSeconds = otpStore.getOtpSessionTtlSeconds(sessionId);
        if (session.locked() || session.verified() || ttlSeconds <= 0) {
            throw new OtpVerificationException("error.otp.invalid");
        }
        if (purpose == OtpPurpose.EMAIL_CHANGE) {
            String email = session.email();
            if (email == null) {
                throw new IllegalArgumentException("Missing email for resend");
            }
            String otp = otpGenerator.generateOtp(purpose);
            String newSessionId = createOtpSession(purpose, otp, session.firstName(), session.lastName(), session.userPasswordHash(), session.mobile(), email, null);
            emailService.sendSimpleEmail(email, "Fitnest Verification Code", "Your Fitnest verification code: " + otp);
            return otpSendResponseMapper.toResponse(newSessionId, otpTtlSeconds, resendCooldownSeconds, getMessage("success.otp.sent"));
        } else if (purpose == OtpPurpose.MOBILE_CHANGE) {
            String mobile = session.mobile();
            if (mobile == null) {
                throw new IllegalArgumentException("Missing mobile for resend");
            }
            String otp = "1111";
            String newSessionId = createOtpSession(purpose, otp, session.firstName(), session.lastName(), session.userPasswordHash(), mobile, session.email(), null);
            smsService.sendSms(mobile, "Your Fitnest verification code: " + otp);
            return otpSendResponseMapper.toResponse(newSessionId, otpTtlSeconds, resendCooldownSeconds, getMessage("success.otp.sent"));
        } else if (purpose == OtpPurpose.REGISTRATION) {
            String mobile = session.mobile();
            if (mobile == null) {
                throw new IllegalArgumentException("Missing mobile for resend");
            }
            String otp = "1111";
            String newSessionId = createOtpSession(purpose, otp, session.firstName(), session.lastName(), session.userPasswordHash(), mobile, session.email(), null);
            smsService.sendSms(mobile, "Your Fitnest verification code: " + otp);
            return otpSendResponseMapper.toResponse(newSessionId, otpTtlSeconds, resendCooldownSeconds, getMessage("success.otp.sent"));
        } else {
            throw new IllegalArgumentException("Unsupported purpose for resend");
        }
    }

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
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
