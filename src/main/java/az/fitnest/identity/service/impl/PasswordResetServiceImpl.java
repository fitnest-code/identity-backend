package az.fitnest.identity.service.impl;

import az.fitnest.identity.constants.OtpPurpose;
import az.fitnest.identity.dto.ForgotPasswordRequest;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.dto.ResetPasswordRequest;
import az.fitnest.identity.dto.ResetPasswordResponse;
import az.fitnest.identity.entity.AuthToken;
import az.fitnest.identity.entity.User;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.service.PasswordResetService;
import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.ResetPasswordTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final OtpService otpService;
    private final ResetPasswordTokenService resetPasswordTokenService;
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final AuthTokenRepository authTokenRepository;
    private final RedisTokenService redisTokenService;

    @Override
    @Transactional
    public OtpSendResponse forgotPassword(ForgotPasswordRequest request) {
        // Early validation
        if (request == null || !StringUtils.hasText(request.getMobile())) {
            // Always respond with generic message to avoid user enumeration
            return OtpSendResponse.builder().message("If the number exists, OTP sent").build();
        }
        String rawMobile = request.getMobile();
        String mobile = az.fitnest.identity.criteria.MobileNumberUtils.normalize(rawMobile);
        OtpSendRequest otpRequest = OtpSendRequest.builder()
                .mobile(mobile)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .build();
        // Send OTP, but always return generic response
        otpService.sendOtp(otpRequest);
        return OtpSendResponse.builder().message("If the number exists, OTP sent").build();
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        // Early validation
        if (request == null || !StringUtils.hasText(request.getResetToken()) ||
            !StringUtils.hasText(request.getNewPassword()) || !StringUtils.hasText(request.getConfirmPassword())) {
            throw new az.fitnest.identity.exception.ValidationException("Invalid request parameters", "VALIDATION_ERROR");
        }
        // Password policy: min length, complexity, breached-password check (pseudo-code)
        String newPassword = request.getNewPassword();
        if (newPassword.length() < 8) {
            throw new az.fitnest.identity.exception.ValidationException("Password must be at least 8 characters", "VALIDATION_ERROR");
        }
        // Add complexity and breached-password checks as needed
        if (!newPassword.equals(request.getConfirmPassword())) {
            throw new az.fitnest.identity.exception.ValidationException("Passwords do not match", "VALIDATION_ERROR");
        }
        String identifier = resetPasswordTokenService.requireIdentifier(request.getResetToken());
        User user = userRepository.findFirstByMobile(identifier)
                .orElseThrow(() -> new az.fitnest.identity.exception.InvalidCredentialsException("Invalid credentials"));
        if (user.isDeleted()) {
            throw new az.fitnest.identity.exception.InvalidCredentialsException("Invalid credentials");
        }
        // Optional: reject if newPassword equals old password
        if (passwordService.verifyPassword(newPassword, user.getPasswordHash()).matches()) {
            throw new az.fitnest.identity.exception.ValidationException("New password must differ from old password", "VALIDATION_ERROR");
        }
        // Hash and set password
        String passwordHash = passwordService.hashPassword(newPassword);
        user.setPasswordHash(passwordHash);
        userRepository.save(user);
        // Consume reset token before revoking tokens
        resetPasswordTokenService.consume(request.getResetToken());
        // Publish event after commit for Redis revocation (pseudo-code)
        // TransactionSynchronizationManager.registerSynchronization(new RedisRevocationEvent(user.getId()));
        revokeAllUserTokens(user.getId());
        return ResetPasswordResponse.builder()
                .message("Password has been reset successfully.")
                .build();
    }

    private void revokeAllUserTokens(Long userId) {
        // Optimize: bulk revoke pattern (pseudo-code)
        // redisTokenService.invalidateTokensBefore(userId, Instant.now());
        List<AuthToken> tokens = authTokenRepository.findByUserId(userId);
        for (AuthToken token : tokens) {
            if (token.getJti() != null) {
                redisTokenService.revokeAccessToken(token.getJti());
            }
        }
        authTokenRepository.deleteByUserId(userId);
        // mark user as having no active sessions
        userRepository.findById(userId).ifPresent(u -> {
            u.setStatus(User.Status.NO_SESSIONS);
            userRepository.save(u);
        });
    }
}
