package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.model.enums.SessionStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.dto.request.ForgotPasswordRequest;
import az.fitnest.identity.dto.request.OtpSendRequest;
import az.fitnest.identity.dto.response.OtpSendResponse;
import az.fitnest.identity.dto.request.ResetPasswordRequest;
import az.fitnest.identity.dto.response.ResetPasswordResponse;
import az.fitnest.identity.model.entity.AuthToken;
import az.fitnest.identity.model.entity.User;
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
    private final org.springframework.context.MessageSource messageSource;
    private final az.fitnest.identity.mapper.OtpSendResponseMapper otpSendResponseMapper;
    private final az.fitnest.identity.mapper.ResetPasswordResponseMapper resetPasswordResponseMapper;

    @Override
    @Transactional
    public OtpSendResponse forgotPassword(ForgotPasswordRequest request) {
        if (request == null || !StringUtils.hasText(request.mobile())) {
            return otpSendResponseMapper.toResponse(null, null, null, getMessage("success.otp.sent"));
        }
        String rawMobile = request.mobile();
        String mobile = az.fitnest.identity.util.MobileNumberUtils.normalize(rawMobile);
        
        userRepository.findFirstByMobile(mobile).ifPresent(user -> {
            if (!user.isHasLocalPassword()) {
                throw new az.fitnest.identity.exception.BadRequestException("error.auth.social_only_account");
            }
        });

        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.PASSWORD_RESET, mobile, null, null);
        return otpService.sendOtp(otpRequest);
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        if (request == null || !StringUtils.hasText(request.resetToken()) ||
                !StringUtils.hasText(request.newPassword())) {
            throw new az.fitnest.identity.exception.ValidationException("error.request.invalid", "VALIDATION_ERROR");
        }
        String newPassword = request.newPassword();
        if (newPassword.length() < 8) {
            throw new az.fitnest.identity.exception.ValidationException("error.service.password_invalid", "VALIDATION_ERROR");
        }

        String identifier = resetPasswordTokenService.requireIdentifier(request.resetToken());
        User user = userRepository.findFirstByMobile(identifier)
                .orElseThrow(() -> new az.fitnest.identity.exception.InvalidCredentialsException("error.auth.invalid_credentials"));
        if (passwordService.verifyPassword(newPassword, user.getPasswordHash()).matches()) {
            throw new az.fitnest.identity.exception.ValidationException("error.service.password_not_allowed", "VALIDATION_ERROR");
        }
        String passwordHash = passwordService.hashPassword(newPassword);
        user.setPasswordHash(passwordHash);
        user.setHasLocalPassword(true);

        if (user.getStatus() == UserStatus.INACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        userRepository.save(user);
        resetPasswordTokenService.consume(request.resetToken());
        revokeAllUserTokens(user.getId());
        return resetPasswordResponseMapper.toResponse(getMessage("success.password.changed"));
    }

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
    }

    private void revokeAllUserTokens(Long userId) {
        List<AuthToken> tokens = authTokenRepository.findByUserId(userId);
        for (AuthToken token : tokens) {
            if (token.getJti() != null) {
                redisTokenService.revokeAccessToken(token.getJti());
            }
        }
        authTokenRepository.deleteByUserId(userId);
        userRepository.findById(userId).ifPresent(u -> {
            u.setSessionStatus(SessionStatus.NO_SESSIONS);
            userRepository.save(u);
        });
    }
}
