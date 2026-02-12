package az.fitnest.identity.service;

import az.fitnest.identity.constants.OtpMessages;
import az.fitnest.identity.constants.OtpPurpose;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.dto.OtpVerifyRequest;
import az.fitnest.identity.dto.OtpVerifyResponse;
import az.fitnest.identity.entity.OtpSessionPayload;
import az.fitnest.identity.entity.OtpVerificationResult;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.OtpRateLimitedException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.EmailService;
import az.fitnest.identity.service.SmsService;
import az.fitnest.identity.service.EmailNormalizationService;
import az.fitnest.identity.service.impl.OtpGenerator;
import az.fitnest.identity.service.impl.OtpRateLimiter;
import az.fitnest.identity.service.impl.OtpSessionIdGenerator;
import az.fitnest.identity.service.impl.OtpStore;
import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.RegistrationTokenService;
import az.fitnest.identity.service.ResetPasswordTokenService;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

public interface OtpService {
    OtpSendResponse sendOtp(OtpSendRequest request);
    OtpSendResponse sendOtp(OtpSendRequest request, String firstName, String lastName, String userPasswordHash, String mobile);
    OtpVerificationResult verifyOtp(String sessionId, String otpCode);
    OtpVerificationResult verifyOtpByIdentifier(String identifier, OtpPurpose purpose, String otpCode);
    OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request);
}
