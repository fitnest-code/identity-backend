package az.fitnest.iam.otp.adapter.service;

import az.fitnest.iam.otp.domain.model.OtpSessionPayload;
import az.fitnest.iam.otp.api.dto.request.OtpSendRequest;
import az.fitnest.iam.otp.api.dto.request.OtpVerifyRequest;
import az.fitnest.iam.otp.api.dto.response.OtpSendResponse;
import az.fitnest.iam.otp.api.dto.response.OtpVerifyResponse;
import az.fitnest.iam.otp.domain.enums.OtpPurpose;
import az.fitnest.iam.shared.exception.BadRequestException;
import az.fitnest.iam.shared.exception.EmailAlreadyRegisteredException;
import az.fitnest.iam.shared.exception.InvalidCredentialsException;
import az.fitnest.iam.shared.exception.OtpRateLimitedException;
import az.fitnest.iam.shared.exception.ResourceNotFoundException;
import az.fitnest.iam.messaging.EmailSender;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.adapter.service.EmailNormalizationService;
import az.fitnest.iam.otp.adapter.store.redis.OtpStore;
import az.fitnest.iam.otp.adapter.service.OtpGenerator;
import az.fitnest.iam.otp.adapter.service.OtpSessionIdGenerator;
import az.fitnest.iam.auth.adapter.service.PasswordService;
import az.fitnest.iam.auth.adapter.service.RegistrationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailNormalizationService emailNormalizationService;
    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final OtpGenerator otpGenerator;
    private final PasswordService passwordService;
    private final OtpSessionIdGenerator otpSessionIdGenerator;
    private final EmailSender emailSender;
    private final RegistrationTokenService registrationTokenService;

    @Value("${otp.ttl-seconds}")
    private int otpTtlSeconds;

    @Value("${otp.resend-cooldown-seconds}")
    private int resendCooldownSeconds;

    public OtpSendResponse sendOtp(OtpSendRequest request) {

        String email = emailNormalizationService.normalize(request.getEmail());
        OtpPurpose purpose = request.getPurpose();

        if (purpose == OtpPurpose.REGISTRATION) {
            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw new EmailAlreadyRegisteredException("Email already registered");
            }
        }

        if (purpose == OtpPurpose.LOGIN) {
            if (!userRepository.existsByEmailIgnoreCase(email)) {
                throw new InvalidCredentialsException("Invalid credentials");
            }
        }

        if (otpStore.isCooldownActive(purpose, email)) {
            Duration remaining = otpStore.getCooldownRemaining(purpose, email);
            throw new OtpRateLimitedException(
                    "OTP rate limited. Retry after " + remaining.getSeconds() + " seconds"
            );
        }

        otpStore.getActiveSessionPointer(purpose, email)
                .ifPresent(otpStore::deleteSession);

        String otp = otpGenerator.generateOtp();
        String otpHash = passwordService.hashPassword(otp);
        String sessionId = otpSessionIdGenerator.generateSessionId();

        OtpSessionPayload payload = OtpSessionPayload.builder()
                .email(email)
                .purpose(purpose)
                .otpHash(otpHash)
                .attempts(0)
                .locked(false)
                .verified(false)
                .createdAt(Instant.now())
                .build();

        otpStore.saveOtpSession(sessionId, payload, otpTtlSeconds);

        otpStore.startCooldown(purpose, email, resendCooldownSeconds);

        otpStore.setActiveSessionPointer(purpose, email, sessionId, otpTtlSeconds);

        emailSender.sendOtp(email, otp, purpose.name());

        return OtpSendResponse.builder()
                .otpSessionId(sessionId)
                .expiresInSeconds(otpTtlSeconds)
                .resendAvailableInSeconds(resendCooldownSeconds)
                .build();
    }

    public String verifyOtp(String sessionId, String otpCode) {
        OtpSessionPayload session = otpStore.getOtpSession(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("OTP session not found or expired"));

        if (session.getLocked()) {
            throw new BadRequestException("OTP session is locked due to too many failed attempts");
        }

        if (session.getVerified()) {
            throw new BadRequestException("OTP already verified");
        }

        int maxAttempts = 5;
        if (session.getAttempts() >= maxAttempts) {
            session.setLocked(true);
            otpStore.updateOtpSession(sessionId, session);
            throw new BadRequestException("OTP session locked due to too many failed attempts");
        }

        boolean isValid = passwordService.verifyPassword(otpCode, session.getOtpHash());
        
        session.setAttempts(session.getAttempts() + 1);
        otpStore.updateOtpSession(sessionId, session);

        if (!isValid) {
            throw new InvalidCredentialsException("Invalid OTP code");
        }

        session.setVerified(true);
        otpStore.updateOtpSession(sessionId, session);

        return session.getEmail();
    }

    public OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request) {
        String email = verifyOtp(request.getOtpSessionId(), request.getOtpCode());
        String registrationToken = registrationTokenService.issueForEmail(email);
        
        return OtpVerifyResponse.builder()
                .verified(true)
                .registrationToken(registrationToken)
                .build();
    }
}
