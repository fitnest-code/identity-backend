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
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.security.RedisTokenService;

import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.RegistrationTokenService;
import az.fitnest.identity.service.ResetPasswordTokenService;
import az.fitnest.identity.service.EmailService;
import az.fitnest.identity.service.TokenIssuanceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import az.fitnest.identity.mapper.OtpSendResponseMapper;
import az.fitnest.identity.mapper.OtpVerifyResponseMapper;
import az.fitnest.identity.model.otp.OtpUserState;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final UserRepository userRepository;
    private final az.fitnest.identity.service.UserProfileGrpcClient userProfileGrpcClient;
    private final TestUserHelper testUserHelper;
    private final OtpStore otpStore;
    private final OtpRateLimiterFacade otpRateLimiter;
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
    private final AuthTokenRepository authTokenRepository;
    private final RedisTokenService redisTokenService;
    @Autowired
    private OtpStateRepository otpStateRepository;
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

    /** When false, non-test phone OTPs are fixed to {@code 0000} (mock SMS). Test users keep {@code 1111}. */
    @Value("${app.sms.enabled:true}")
    private boolean smsEnabled;

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
        OtpPurpose purpose = request.getPurpose();
        if (purpose == null) {
            purpose = resolvePurpose(request);
            request.setPurpose(purpose);
        }

        String identifier = (purpose == OtpPurpose.EMAIL_CHANGE) ? request.getEmail() : request.getMobile();
        if (identifier == null) {
            throw new IllegalArgumentException(purpose == OtpPurpose.EMAIL_CHANGE ? getMessage("error.service.missing_email") : getMessage("error.service.missing_mobile"));
        }

        return sendOtp(request, null, null, null, null);
    }

    @Override
    @Async
    public CompletableFuture<OtpSendResponse> sendOtpAsync(OtpSendRequest request) {
        return CompletableFuture.completedFuture(sendOtp(request));
    }

    private OtpPurpose resolvePurpose(OtpSendRequest request) {
        if (request.getEmail() != null) return OtpPurpose.EMAIL_CHANGE;
        if (request.getMobile() != null) return OtpPurpose.MOBILE_CHANGE;
        return OtpPurpose.REGISTRATION;
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

        if (userId == null) {
            validateRateLimit(purpose, identifier);
        }

        boolean exists = (purpose == OtpPurpose.EMAIL_CHANGE)
                ? userProfileGrpcClient.getUserByEmail(email) != null
                : userRepository.findFirstByMobile(mobileNumber).isPresent();

        if (exists && (purpose == OtpPurpose.ADD_NUMBER_GOOGLE || purpose == OtpPurpose.ADD_NUMBER_APPLE)) {
            Optional<az.fitnest.identity.model.entity.User> existingUserOpt = userRepository.findFirstByMobile(mobileNumber);
            if (existingUserOpt.isPresent()) {
                String existingEmail = null;
                try {
                    var profile = userProfileGrpcClient.getUserProfileDetails(existingUserOpt.get().getId());
                    if (profile != null) {
                        existingEmail = profile.getEmail();
                    }
                } catch (Exception ignored) {}
                if (existingEmail == null || existingEmail.trim().isEmpty()) {
                    exists = false;
                }
            }
        }

        boolean shouldSendOtp = doesPurposeMatchExistence(purpose, exists);

        if (!shouldSendOtp) {
            return createFakeSessionResponse();
        }

        invalidateActiveSession(purpose, identifier, userId);

        boolean isTestUser = testUserHelper.isTestIdentifier(identifier) || testUserHelper.isTestUserId(userId);
        // Test users: always 1111 (prod + development). SMS disabled: mock OTP 0000 for everyone else (phone).
        String otp;
        if (isTestUser) {
            otp = "1111";
        } else if (!smsEnabled && purpose != OtpPurpose.EMAIL_CHANGE) {
            otp = "0000";
        } else {
            otp = enforceOtpLength(otpGenerator.generateOtp(purpose));
        }
        String sessionId = request.getSessionId() != null ? request.getSessionId() : createOtpSession(purpose, otp, firstName, lastName, userPasswordHash, mobileNumber, email, userId);

        if (!isTestUser) {
            if (purpose == OtpPurpose.EMAIL_CHANGE) {
                java.util.Map<String, Object> vars = new java.util.HashMap<>();
                vars.put("otp", otp);
                emailService.sendHtmlEmail(email, "Fitnest Təsdiq Kodu", "otp.html", vars);
            } else {
                // When SMS_ENABLED=false, notifications-backend mocks LSIM; OTP above is 0000.
                smsService.sendSms(mobileNumber, "Təhlükəsizlik kodunuzu heç kimlə paylaşmayın!\nCode: " + otp);
            }
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
        if (purpose == OtpPurpose.REGISTRATION || purpose == OtpPurpose.EMAIL_CHANGE || purpose == OtpPurpose.MOBILE_CHANGE ||
            purpose == OtpPurpose.ADD_NUMBER_GOOGLE || purpose == OtpPurpose.ADD_NUMBER_APPLE) {
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
        if (testUserHelper.isTestIdentifier(identifier)) {
            return;
        }
        OtpRateLimiter.RateLimitResult rateLimitResult = otpRateLimiter.checkRateLimit(purpose, identifier);
        if (!rateLimitResult.allowed()) {
            long waitTimeSeconds = rateLimitResult.waitTimeSeconds();
            String messageKey = "error.otp.rate_limit_generic";
            String message;

            if (waitTimeSeconds >= 60) {
                messageKey = "error.otp.rate_limit_minutes";
                message = getMessage(messageKey, (waitTimeSeconds + 59) / 60);
            } else if (waitTimeSeconds > 0) {
                messageKey = "error.otp.rate_limit_seconds";
                message = getMessage(messageKey, waitTimeSeconds);
            } else {
                message = getMessage(messageKey);
            }

            throw new OtpRateLimitedException(message, messageKey, waitTimeSeconds);
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
        boolean isTestUser = testUserHelper.isTestUserId(session.userId())
                || testUserHelper.isTestIdentifier(session.mobile())
                || testUserHelper.isTestIdentifier(session.email());

        // Test users (any env): 1111. Mock SMS (SMS_ENABLED=false): also accept 0000.
        boolean isValid = hashOtp(otpCode).equals(session.otpHash())
                || (isTestUser && "1111".equals(otpCode))
                || (!smsEnabled && "0000".equals(otpCode));
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
    @Transactional
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

            String deviceType = "Web";
            String activeJti = redisTokenService.getActiveSession(user.getId(), deviceType);
            if (activeJti != null) {
                redisTokenService.revokeAccessToken(activeJti);
                authTokenRepository.deleteByJti(activeJti);
            }

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
        } else if (verificationResult.purpose() == OtpPurpose.EMAIL_CHANGE || verificationResult.purpose() == OtpPurpose.MOBILE_CHANGE ||
                   verificationResult.purpose() == OtpPurpose.ADD_NUMBER_GOOGLE || verificationResult.purpose() == OtpPurpose.ADD_NUMBER_APPLE) {
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
            throw new IllegalArgumentException("Missing identifier for resend");
        }

        validateRateLimit(purpose, identifier);

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
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("otp", otp);
            emailService.sendHtmlEmail(email, "Fitnest Təsdiq Kodu", "otp.html", vars);
        } else {
            smsService.sendSms(mobile, "Təhlükəsizlik kodunuzu heç kimlə paylaşmayın!\nCode: " + otp);
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
            throw new IllegalStateException("Security configuration error", e);
        }
    }

    private String getOtpGlobalKey(Long userId) {
        if (userId != null) {
            return "user:" + userId;
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();

            String ip = req.getHeader("X-Client-IP");
            if (ip == null || ip.isBlank()) {
                ip = req.getHeader("X-Forwarded-For");
                if (ip != null && !ip.isBlank()) {
                    ip = ip.split(",")[0].trim();
                } else {
                    ip = req.getRemoteAddr();
                }
            }

            String userAgent = req.getHeader("User-Agent");
            if (userAgent != null && !userAgent.isBlank()) {
                ip = ip + ":" + Math.abs(userAgent.hashCode());
            }

            return "ip:" + ip;
        }
        return "ip:unknown";
    }

    private OtpUserState getOrInitOtpUserState(String key) {
        return otpStateRepository.get(key).orElseGet(() -> new OtpUserState(key));
    }
}

