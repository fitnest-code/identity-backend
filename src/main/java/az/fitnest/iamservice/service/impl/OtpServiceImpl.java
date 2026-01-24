package az.fitnest.iamservice.service.impl;

import az.fitnest.iamservice.dto.common.OtpSessionPayload;
import az.fitnest.iamservice.dto.request.OtpSendRequest;
import az.fitnest.iamservice.dto.response.OtpSendResponse;
import az.fitnest.iamservice.enums.OtpPurpose;
import az.fitnest.iamservice.exception.EmailAlreadyRegisteredException;
import az.fitnest.iamservice.exception.InvalidCredentialsException;
import az.fitnest.iamservice.exception.OtpRateLimitedException;
import az.fitnest.iamservice.messaging.EmailSender;
import az.fitnest.iamservice.repository.UserRepository;
import az.fitnest.iamservice.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final EmailNormalizationService emailNormalizationService;
    private final UserRepository userRepository;
    private final RedisOtpStoreService redisOtpStoreService;
    private final OtpGenerator otpGenerator;
    private final PasswordService passwordService;
    private final OtpSessionIdGenerator otpSessionIdGenerator;
    private final EmailSender emailSender;

    @Value("${otp.ttl-seconds}")
    private int otpTtlSeconds;

    @Value("${otp.resend-cooldown-seconds}")
    private int resendCooldownSeconds;

    @Override
    public OtpSendResponse sendOtp(OtpSendRequest request) {

        String email = emailNormalizationService.normalize(request.getEmail());
        OtpPurpose purpose = request.getPurpose();

        if (purpose == OtpPurpose.REGISTRATION) {
            if (userRepository.existsByEmail(email)) {
                throw new EmailAlreadyRegisteredException("Email already registered");
            }
        }

        if (purpose == OtpPurpose.LOGIN) {
            if (!userRepository.existsByEmail(email)) {
                throw new InvalidCredentialsException("Invalid credentials");
            }
        }

        if (redisOtpStoreService.isCooldownActive(purpose, email)) {
            Duration remaining = redisOtpStoreService.getCooldownRemaining(purpose, email);
            throw new OtpRateLimitedException(
                    "OTP rate limited. Retry after " + remaining.getSeconds() + " seconds"
            );
        }

        redisOtpStoreService.getActiveSessionPointer(purpose, email)
                .ifPresent(redisOtpStoreService::deleteSession);

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
                .build();

        redisOtpStoreService.saveOtpSession(
                sessionId, payload, otpTtlSeconds);

        redisOtpStoreService.startCooldown(
                purpose, email, otpTtlSeconds);

        redisOtpStoreService.setActiveSessionPointer(
                purpose, email, sessionId, otpTtlSeconds
        );

        emailSender.sendOtp(email, otp, purpose.name());

        return OtpSendResponse.builder()
                .otpSessionId(sessionId)
                .expiresInSeconds(otpTtlSeconds)
                .resendAvailableInSeconds(resendCooldownSeconds)
                .build();
    }
}
