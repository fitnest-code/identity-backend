package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.response.PasswordVerificationResultResponse;
import az.fitnest.identity.service.*;
import az.fitnest.identity.util.TokenHasher;

import az.fitnest.identity.dto.request.LoginRequest;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.dto.response.RefreshResponse;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.model.enums.SessionStatus;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.security.JwtService;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.util.DeviceDetector;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final AuthTokenRepository authTokenRepository;
    private final RedisTokenService redisTokenService;
    private final TokenIssuanceService tokenIssuanceService;
    private final OtpService otpService;
    private final TokenHasher tokenHasher;
    private final MessageSource messageSource;

    @Value("${auth.account-lock.max-failed-attempts:5}")
    private int maxFailedLoginAttempts;

    @Value("${auth.account-lock.lock-duration-minutes:30}")
    private int accountLockDurationMinutes;

    @Override
    public Object login(LoginRequest request) {
        String mobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.mobile());
        AuthenticationResult result = authenticate(mobile, request.password());

        if (result.status() == AuthenticationStatus.REACTIVATION_REQUIRED) {
            az.fitnest.identity.dto.request.OtpSendRequest otpRequest = new az.fitnest.identity.dto.request.OtpSendRequest(
                    az.fitnest.identity.model.enums.OtpPurpose.REACTIVATION,
                    result.user().getMobile(),
                    null,
                    null
            );
            az.fitnest.identity.dto.response.OtpSendResponse otpResponse = otpService.sendOtp(otpRequest);
            az.fitnest.identity.dto.response.OtpSendResponse reactivationResponse = new az.fitnest.identity.dto.response.OtpSendResponse(
                otpResponse.otpSessionId(),
                otpResponse.expiresInSeconds(),
                otpResponse.resendAvailableInSeconds(),
                getMessage("success.otp.reactivation_sent")
            );
            return reactivationResponse;
        }

        String activeJti = redisTokenService.getActiveSession(result.user().getId());
        if (activeJti != null) {
            redisTokenService.revokeAccessToken(activeJti);
            redisTokenService.removeActiveSession(result.user().getId());
        }

        User user = result.user();
        if (user.getSessionStatus() != SessionStatus.HAVE_SESSIONS) {
            user.setSessionStatus(SessionStatus.HAVE_SESSIONS);
            userRepository.save(user);
        }

        return tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());
    }

    @Transactional
    public AuthenticationResult authenticate(String mobile, String password) {
        User user = userRepository.findFirstByMobile(mobile)
                .orElseThrow(() -> new InvalidCredentialsException("error.auth.invalid_credentials"));

        Instant now = Instant.now();

        if (user.getStatus() == UserStatus.DELETED) {
            throw new InvalidCredentialsException("error.auth.account_deleted");
        }

        if (isAccountLocked(user, now)) {
            throw new InvalidCredentialsException("error.auth.account_locked");
        }

        if (user.getStatus() == UserStatus.INACTIVE && user.getInactiveAt() != null) {
            if (user.getInactiveAt().plus(java.time.Duration.ofDays(30)).isAfter(now)) {
                verifyPassword(user, password);
                return new AuthenticationResult(user, AuthenticationStatus.REACTIVATION_REQUIRED);
            } else {
                throw new InvalidCredentialsException("error.auth.account_deleted");
            }
        }

        verifyPassword(user, password);

        boolean updated = false;
        PasswordVerificationResultResponse verification = passwordService.verifyPassword(password, user.getPasswordHash());
        if (verification.upgradeRecommended()) {
            user.setPasswordHash(passwordService.hashPassword(password));
            updated = true;
        }

        if (user.getSessionStatus() == SessionStatus.NO_SESSIONS) {
            user.setSessionStatus(SessionStatus.HAVE_SESSIONS);
            updated = true;
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setStatus(UserStatus.ACTIVE);
        updated = true;

        if (updated) {
            userRepository.save(user);
        }

        return new AuthenticationResult(user, AuthenticationStatus.SUCCESS);
    }

    private void verifyPassword(User user, String password) {
        PasswordVerificationResultResponse verification = passwordService.verifyPassword(password, user.getPasswordHash());
        if (user.getPasswordHash() == null || !verification.matches()) {
            incrementFailedLoginAttempts(user.getId(), user.getFailedLoginAttempts(), Instant.now());
            throw new InvalidCredentialsException("error.auth.invalid_credentials");
        }
    }

    @Override
    @Transactional
    public RefreshResponse refresh(String refreshToken) {
        log.info("[refresh] Received refresh token request. RefreshToken: {}", refreshToken);
        Long userId;
        Instant expiration;
        try {
            userId = jwtService.parseUserId(refreshToken, "refresh");
            expiration = jwtService.parseExpiration(refreshToken);
            log.info("[refresh] Parsed refresh token. userId: {}, expiration: {}", userId, expiration);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("[refresh] Failed to parse refresh token: {}", e.getMessage());
            throw new UnauthorizedException("error.auth.invalid_credentials");
        }

        if (expiration.isBefore(Instant.now())) {
            log.warn("[refresh] Refresh token expired for userId: {}. Expiration: {}", userId, expiration);
            throw new UnauthorizedException("error.auth.invalid_credentials");
        }

        User user = internalRefresh(userId, refreshToken);

        LoginResponse tokens = tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());

        if (user.getSessionStatus() != SessionStatus.HAVE_SESSIONS) {
            user.setSessionStatus(SessionStatus.HAVE_SESSIONS);
            userRepository.save(user);
        }

        RefreshResponse response = new RefreshResponse(tokens.accessToken(), tokens.refreshToken());
        log.info("[refresh] Token refresh successful for userId: {}. New AccessToken: {}, New RefreshToken: {}",
                userId, response.accessToken(), response.refreshToken());
        return response;
    }

    private User internalRefresh(Long userId, String refreshToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[refresh] User not found for userId: {}", userId);
                    return new UnauthorizedException("error.auth.invalid_credentials");
                });

        Instant now = Instant.now();
        if (user.getStatus() == UserStatus.INACTIVE || (user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))) {
            log.warn("[refresh] User status prevents refresh. userId: {}, status: {}, lockedUntil: {}",
                    userId, user.getStatus(), user.getLockedUntil());
            throw new UnauthorizedException("error.auth.invalid_credentials");
        }

        String refreshTokenHash = tokenHasher.hash(refreshToken);
        int consumed = authTokenRepository.consumeRefreshToken(userId, refreshTokenHash, now);

        if (consumed == 0) {
            log.warn("[refresh] Refresh token not found, already consumed, or expired in DB. userId: {}, tokenHash: {}",
                    userId, refreshTokenHash);
            throw new UnauthorizedException("error.auth.invalid_credentials");
        }

        log.info("[refresh] Refresh token consumed successfully in DB for userId: {}", userId);
        return user;
    }

    @Override
    public void logout(String accessToken) {
        try {
            Long userId = jwtService.parseUserId(accessToken);
            String jti = jwtService.parseJti(accessToken);

            redisTokenService.revokeAccessToken(jti);
            String activeJti = redisTokenService.getActiveSession(userId);
            if (jti.equals(activeJti)) {
                redisTokenService.removeActiveSession(userId);
            }

            internalLogout(userId, accessToken);
        } catch (Exception e) {
            log.warn("Error during logout for token: {}", e.getMessage());
        }
    }

    @Override
    public void logoutFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("error.auth.invalid_header");
        }
        String token = authHeader.substring(7);
        logout(token);
    }

    @Transactional
    public void internalLogout(Long userId, String accessToken) {
        try {
            authTokenRepository.deleteByAccessTokenHash(tokenHasher.hash(accessToken));
        } catch (Exception e) {
            log.error("Failed to delete token from DB during logout for userId {}", userId, e);
        }

        userRepository.markNoSessionsIfNone(userId, SessionStatus.NO_SESSIONS);
    }

    private void incrementFailedLoginAttempts(Long userId, int currentAttempts, Instant now) {
        int attempts = currentAttempts + 1;

        if (attempts >= maxFailedLoginAttempts) {
            userRepository.updateLockStatus(
                    userId,
                    attempts,
                    now.plus(java.time.Duration.ofMinutes(accountLockDurationMinutes)),
                    UserStatus.LOCKED
            );
        } else {
            userRepository.incrementFailedLoginAttempts(userId);
        }
    }

    private void resetFailedLoginAttempts(Long userId) {
        userRepository.updateLockStatus(userId, 0, null, UserStatus.ACTIVE);
    }

    private boolean isAccountLocked(User user, Instant now) {
        return user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
    }

    private enum AuthenticationStatus {SUCCESS, REACTIVATION_REQUIRED}

    private record AuthenticationResult(User user, AuthenticationStatus status) {
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

}
