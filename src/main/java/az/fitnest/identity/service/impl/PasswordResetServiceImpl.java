package az.fitnest.identity.service.impl;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;

import az.fitnest.identity.dto.ForgotPasswordRequest;
import az.fitnest.identity.dto.ResetPasswordRequest;
import az.fitnest.identity.dto.ResetPasswordResponse;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.entity.AuthToken;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.constants.OtpPurpose;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
        @Override
    public OtpSendResponse forgotPassword(ForgotPasswordRequest request) {
        String rawMobile = request.getMobile();
        String mobile = az.fitnest.identity.criteria.MobileNumberUtils.normalize(rawMobile);
        
        OtpSendRequest otpRequest = OtpSendRequest.builder()
                .mobile(mobile)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .build();
        
        return otpService.sendOtp(otpRequest);
    }

    @Transactional
        @Override
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
