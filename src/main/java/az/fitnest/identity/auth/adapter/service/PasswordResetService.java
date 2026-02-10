package az.fitnest.identity.auth.adapter.service;

import az.fitnest.identity.auth.api.dto.request.ForgotPasswordRequest;
import az.fitnest.identity.auth.api.dto.request.ResetPasswordRequest;
import az.fitnest.identity.auth.api.dto.response.ForgotPasswordResponse;
import az.fitnest.identity.auth.api.dto.response.ResetPasswordResponse;
import az.fitnest.identity.auth.api.dto.response.VerifyOtpForPasswordResetResponse;
import az.fitnest.identity.auth.adapter.persistence.AuthTokenRepository;
import az.fitnest.identity.auth.domain.model.AuthToken;
import az.fitnest.identity.otp.adapter.service.OtpService;
import az.fitnest.identity.otp.api.dto.request.OtpSendRequest;
import az.fitnest.identity.otp.api.dto.request.OtpVerifyRequest;
import az.fitnest.identity.otp.domain.enums.OtpPurpose;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.shared.exception.InvalidCredentialsException;
import az.fitnest.identity.user.adapter.persistence.UserRepository;
import az.fitnest.identity.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final OtpService otpService;
    private final ResetPasswordTokenService resetPasswordTokenService;
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final AuthTokenRepository authTokenRepository;
    private final RedisTokenService redisTokenService;

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String rawMobile = request.getMobile();
        String mobile = az.fitnest.identity.shared.util.MobileNumberUtils.normalize(rawMobile);
        
        if (userRepository.findByMobileIncludingDeleted(mobile).isEmpty()) {
           // Don't reveal user existence
           return ForgotPasswordResponse.builder()
                   .message("If an account exists with this mobile number, an OTP code has been sent.")
                   .build();
        }

        OtpSendRequest otpRequest = OtpSendRequest.builder()
                .mobile(mobile)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .build();
        
        otpService.sendOtp(otpRequest);
        
        return ForgotPasswordResponse.builder()
                .message("If an account exists with this mobile number, an OTP code has been sent.")
                .build();
    }

    @Transactional
    public VerifyOtpForPasswordResetResponse verifyOtpForPasswordReset(OtpVerifyRequest request) {
        String resetToken = otpService.verifyOtpAndIssueResetToken(
                request.getOtpSessionId(),
                request.getOtpCode()
        );
        
        return VerifyOtpForPasswordResetResponse.builder()
                .resetToken(resetToken)
                .message("OTP verified. You can now reset your password.")
                .build();
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        String identifier = resetPasswordTokenService.requireIdentifier(request.getResetToken());
        
        User user = userRepository.findByMobileIncludingDeleted(identifier)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.isDeleted()) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        
        String passwordHash = passwordService.hashPassword(request.getNewPassword());
        user.setPasswordHash(passwordHash);
        userRepository.save(user);
        
        revokeAllUserTokens(user.getId());
        
        resetPasswordTokenService.consume(request.getResetToken());
        
        return ResetPasswordResponse.builder()
                .message("Password has been reset successfully.")
                .build();
    }

    private void revokeAllUserTokens(Long userId) {
        List<AuthToken> tokens = authTokenRepository.findByUserId(userId);
        for (AuthToken token : tokens) {
            redisTokenService.revokeAccessToken(token.getAccessToken());
        }
        authTokenRepository.deleteByUserId(userId);
    }
}
