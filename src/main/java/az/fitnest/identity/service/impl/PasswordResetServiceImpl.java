package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.model.enums.SessionStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.dto.ForgotPasswordRequest;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.dto.ResetPasswordRequest;
import az.fitnest.identity.dto.ResetPasswordResponse;
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
    @Override
    @Transactional
    public OtpSendResponse forgotPassword(ForgotPasswordRequest request) {
        if (request == null || !StringUtils.hasText(request.mobile())) {
            return new OtpSendResponse(null, null, null, getMessage("error.otp_sent_if_exists"));
        }
        String rawMobile = request.mobile();
        String mobile = az.fitnest.identity.util.MobileNumberUtils.normalize(rawMobile);
        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.PASSWORD_RESET, mobile, null);
        return otpService.sendOtp(otpRequest);
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        if (request == null || !StringUtils.hasText(request.resetToken()) ||
                !StringUtils.hasText(request.newPassword())) {
            throw new az.fitnest.identity.exception.ValidationException("error.invalid_request", "VALIDATION_ERROR");
        }
        String newPassword = request.newPassword();
        if (newPassword.length() < 8) {
            throw new az.fitnest.identity.exception.ValidationException("error.password_min_length", "VALIDATION_ERROR");
        }

        String identifier = resetPasswordTokenService.requireIdentifier(request.resetToken());
        User user = userRepository.findFirstByMobile(identifier)
                .orElseThrow(() -> new az.fitnest.identity.exception.InvalidCredentialsException("error.invalid_credentials"));
        if (passwordService.verifyPassword(newPassword, user.getPasswordHash()).matches()) {
            throw new az.fitnest.identity.exception.ValidationException("error.password_must_be_different", "VALIDATION_ERROR");
        }
        String passwordHash = passwordService.hashPassword(newPassword);
        user.setPasswordHash(passwordHash);

        if (user.isDeactivated()) {
            user.setStatus(UserStatus.ACTIVE);
            user.setDeactivationReason(null);
            user.setDeactivatedAt(null);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        userRepository.save(user);
        resetPasswordTokenService.consume(request.resetToken());
        revokeAllUserTokens(user.getId());
        return new ResetPasswordResponse(getMessage("error.password_reset_success"));
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
