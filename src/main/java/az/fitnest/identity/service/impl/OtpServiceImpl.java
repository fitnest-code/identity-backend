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
import az.fitnest.identity.repository.OtpStateRepository;

import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.RegistrationTokenService;
import az.fitnest.identity.service.ResetPasswordTokenService;
import az.fitnest.identity.service.EmailService;
import az.fitnest.identity.service.TokenIssuanceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import az.fitnest.identity.model.otp.OtpUserState;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

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
    private final PhoneNormalizer phoneNormalizer;

    @Autowired
    private OtpStateRepository otpStateRepository;

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

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

    @Value("${otp.session-lock-seconds}")
    private int sessionLockSeconds;

    @PostConstruct
    private void validateConfiguration() {
        if (resendCooldownSeconds < minCooldownSeconds) {
            throw new IllegalStateException(
                    "Configuration error: otp.resend-cooldown-seconds (" + resendCooldownSeconds +
                            ") must be >= otp.rate-limit.min-cooldown-seconds (" + minCooldownSeconds + ")"
            );
        }
    }

    @PostConstruct
    private void logRateLimitConfig() {
        log.info("OTP Rate Limit Config: maxAttempts={}, windowMinutes={}, cooldownSeconds={}, dailyMaxAttempts={}, minCooldownSeconds={}, errorMessageThresholdSeconds={}",
                otpRateLimiter.getProperties().getMaxAttempts(),
                otpRateLimiter.getProperties().getWindowMinutes(),
                otpRateLimiter.getProperties().getCooldownSeconds(),
                otpRateLimiter.getProperties().getDailyMaxAttempts(),
                otpRateLimiter.getProperties().getMinCooldownSeconds(),
                otpRateLimiter.getProperties().getErrorMessageThresholdSeconds());
    }

    @Override
    public OtpSendResponse sendOtp(OtpSendRequest request) {
        OtpPurpose purpose = request.getPurpose();
        if (purpose == null) {
            if (request.getEmail() != null) {
                purpose = OtpPurpose.EMAIL_CHANGE;
            } else if (request.getMobile() != null) {
                purpose = OtpPurpose.MOBILE_CHANGE;
            } else {
                purpose = OtpPurpose.REGISTRATION;
            }
            request.setPurpose(purpose);
        }
        String globalKey = getOtpGlobalKey(az.fitnest.identity.util.UserContext.getCurrentUserId());
        OtpUserState state = getOrInitOtpUserState(globalKey);
        Instant now = Instant.now(clock);
        if (state.getLastSentAt() != null && state.getResendCount() != null && state.getResendCount() > 0) {
            long cooldown = 60L * state.getResendCount();
            if (now.isBefore(state.getLastSentAt().plusSeconds(cooldown))) {
                long wait = state.getLastSentAt().plusSeconds(cooldown).getEpochSecond() - now.getEpochSecond();
                throw new OtpRateLimitedException(getMessage("error.otp.resend_cooldown"), wait);
            }
        }
        if (state.getDailySendCount() != null && state.getDailySendCount() >= 10) {
            throw new OtpRateLimitedException(getMessage("error.otp.daily_limit"), 24 * 3600);
        }
        state.setResendCount(state.getResendCount() == null ? 1 : state.getResendCount() + 1);
        state.setLastSentAt(now);
        state.setDailySendCount(state.getDailySendCount() == null ? 1 : state.getDailySendCount() + 1);
        otpStateRepository.save(state);
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

        String otp = enforceOtpLength(otpGenerator.generateOtp(purpose));
        String sessionId = request.getSessionId() != null ? request.getSessionId() : createOtpSession(purpose, otp, firstName, lastName, userPasswordHash, mobileNumber, email, userId);

        if (purpose == OtpPurpose.EMAIL_CHANGE) {
            emailService.sendSimpleEmail(email, "Fitnest Verification Code", "Your Fitnest verification code: " + otp);
        } else {
            smsService.sendSms(mobileNumber, "Your Fitnest verification code: " + otp);
        }
        int resendCount = 1;
        int cooldown = 60 * resendCount;
        return otpSendResponseMapper.toResponse(sessionId, otpTtlSeconds, cooldown, getMessage("success.otp.sent"));
    }

    private String enforceOtpLength(String otp) {
        if (otp == null || !otp.matches("\\d{4}")) {
            throw new IllegalStateException("OTP must be exactly 4 digits");
        }
        return otp;
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
        log.info("[validateRateLimit] Checking rate limit for purpose={}, identifier={}", purpose, identifier);
        OtpRateLimiter.RateLimitResult rateLimitResult = otpRateLimiter.checkRateLimit(purpose, identifier);
        log.info("[validateRateLimit] Rate limit result: allowed={}, waitTimeSeconds={}", rateLimitResult.allowed(), rateLimitResult.waitTimeSeconds());
        if (!rateLimitResult.allowed()) {
            long waitTimeSeconds = rateLimitResult.waitTimeSeconds();
            String message = getMessage("error.otp.rate_limit_generic");
            log.warn("[validateRateLimit] Rate limit exceeded for purpose={}, identifier={}, waitTimeSeconds={}", purpose, identifier, waitTimeSeconds);
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
                .lockedUntil(Instant.now(clock).plusSeconds(sessionLockSeconds))
                .userId(userId)
                .build();
        String identifier = (purpose == OtpPurpose.EMAIL_CHANGE) ? email : mobile;
        log.info("Creating OTP session: sessionId={}, purpose={}, otp={}, otpHash={}, identifier={}, ttl={}, payload={}", sessionId, purpose, otp, otpHash, identifier, otpTtlSeconds, payload);
        otpStore.saveOtpSessionAtomically(purpose, identifier, sessionId, payload, otpTtlSeconds);
        if (userId != null) {
            otpStore.setActiveSessionPointer(purpose, "user:" + userId, sessionId, otpTtlSeconds);
            log.info("Set active session pointer for userId={}, sessionId={}, purpose={}, ttl={}", userId, sessionId, purpose, otpTtlSeconds);
        }
        return sessionId;
    }

    @Override
    public OtpVerificationResult verifyOtp(String sessionId, String otpCode) {
        log.info("Verifying OTP: sessionId={}, otpCode={}", sessionId, otpCode);
        Optional<OtpSessionPayload> sessionOpt = otpStore.getSessionForVerification(sessionId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found for sessionId={}", sessionId);
            throw new OtpVerificationException("error.otp.invalid");
        }
        OtpSessionPayload session = sessionOpt.get();
        log.info("Session payload: {}", session);
        if (session.locked()) {
            log.warn("Session is locked: sessionId={}", sessionId);
            throw new OtpVerificationException("error.otp.locked");
        }
        if (session.verified()) {
            log.warn("Session is already verified: sessionId={}", sessionId);
            throw new OtpVerificationException("error.otp.already_verified");
        }
        boolean isValid = hashOtp(otpCode).equals(session.otpHash());
        log.info("OTP hash comparison: inputHash={}, storedHash={}, isValid={}", hashOtp(otpCode), session.otpHash(), isValid);
        OtpStore.VerifyOtpResult result = otpStore.verifyOtpAndUpdate(sessionId, maxVerifyAttempts, isValid);
        log.info("VerifyOtpResult: found={}, status={}, session={}", result.isFound(), result.getStatus(), result.getSession());
        if (!result.isFound()) {
            log.warn("Session not found after verifyOtpAndUpdate: sessionId={}", sessionId);
            throw new OtpVerificationException("error.otp.invalid");
        }
        if (result.isLocked()) {
            log.warn("Session locked after verifyOtpAndUpdate: sessionId={}", sessionId);
            throw new OtpVerificationException("error.otp.locked");
        }
        if (result.isAlreadyVerified()) {
            log.warn("Session already verified after verifyOtpAndUpdate: sessionId={}", sessionId);
            throw new OtpVerificationException("error.otp.already_verified");
        }
        if (result.isExpired()) {
            log.warn("Session expired after verifyOtpAndUpdate: sessionId={}", sessionId);
            throw new OtpVerificationException("error.otp.invalid");
        }
        if (!isValid) {
            log.warn("OTP code is invalid: sessionId={}, otpCode={}", sessionId, otpCode);
            throw new OtpVerificationException("error.otp.invalid");
        }
        OtpSessionPayload verifiedSession = result.getSession();
        log.info("OTP verified successfully: sessionId={}, userId={}, purpose={}", sessionId, verifiedSession.userId(), verifiedSession.purpose());
        return OtpVerificationResult.builder()
                .purpose(verifiedSession.purpose())
                .firstName(verifiedSession.firstName())
                .lastName(verifiedSession.lastName())
                .passwordHash(verifiedSession.userPasswordHash())
                .mobile(verifiedSession.mobile())
                .email(verifiedSession.email())
                .userId(verifiedSession.userId())
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

        String identifier = (verificationResult.purpose() == OtpPurpose.EMAIL_CHANGE)
                ? verificationResult.email() : verificationResult.mobile();

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
        OtpSessionPayload session = otpStore.getSessionForVerification(sessionId)
                .orElseThrow(() -> new OtpVerificationException("error.otp.invalid"));

        long ttlSeconds = otpStore.getOtpSessionTtlSeconds(sessionId);
        if (session.locked() || session.verified()) {
            throw new OtpVerificationException("error.otp.invalid");
        }

        String email = session.email();
        String mobile = session.mobile();
        String identifier;
        if (purpose == OtpPurpose.EMAIL_CHANGE) {
            identifier = (email != null) ? email.trim().toLowerCase() : null;
        } else {
            identifier = (mobile != null) ? phoneNormalizer.normalizeAzerbaijanPhoneNumber(mobile) : null;
        }

        if (identifier == null || identifier.isEmpty()) {
            log.warn("[resendOtp] Missing or invalid identifier for rate limit: purpose={}, sessionId={}", purpose, sessionId);
            throw new IllegalArgumentException("Missing identifier for resend");
        }

        log.info("[resendOtp] Checking rate limit: purpose={}, identifier={}, sessionId={}", purpose, identifier, sessionId);
        validateRateLimit(purpose, identifier);

        log.info("[resendOtp] TTL seconds for session {}: {}", sessionId, ttlSeconds);
        int resendCount = (session.resendCount() != null) ? session.resendCount() + 1 : 1;
        int incrementalCooldown = 60 * resendCount;

        String otp = enforceOtpLength(otpGenerator.generateOtp(purpose));
        String otpHash = hashOtp(otp);

        OtpSessionPayload updatedSession = OtpSessionPayload.builder()
                .purpose(session.purpose())
                .otpHash(otpHash)
                .attempts(0)
                .locked(false)
                .verified(false)
                .createdAt(java.time.Instant.now(clock))
                .firstName(session.firstName())
                .lastName(session.lastName())
                .userPasswordHash(session.userPasswordHash())
                .mobile(session.mobile())
                .email(session.email())
                .lockedUntil(session.lockedUntil())
                .userId(session.userId())
                .resendCount(resendCount)
                .build();
        otpStore.updateOtpSession(sessionId, updatedSession);

        if (purpose == OtpPurpose.EMAIL_CHANGE) {
            emailService.sendSimpleEmail(email, "Fitnest Verification Code", "Your Fitnest verification code: " + otp);
        } else {
            smsService.sendSms(mobile, "Your Fitnest verification code: " + otp);
        }

        return otpSendResponseMapper.toResponse(sessionId, otpTtlSeconds, incrementalCooldown, getMessage("success.otp.sent"));
    }

    @Override
    public OtpVerificationResult getOtpSession(String sessionId) {
        OtpSessionPayload session = otpStore.getSessionForVerification(sessionId)
                .orElseThrow(() -> new OtpVerificationException("error.otp.invalid"));

        return OtpVerificationResult.builder()
                .purpose(session.purpose())
                .firstName(session.firstName())
                .lastName(session.lastName())
                .passwordHash(session.userPasswordHash())
                .mobile(session.mobile())
                .email(session.email())
                .build();
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

    private String getOtpGlobalKey(Long userId) {
        if (userId != null) {
            return "user:" + userId;
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = req.getRemoteAddr();
            } else {
                ip = ip.split(",")[0].trim();
            }
            return "ip:" + ip;
        }
        return "ip:unknown";
    }

    private OtpUserState getOrInitOtpUserState(String key) {
        return otpStateRepository.get(key).orElseGet(() -> new OtpUserState(key));
    }
}
