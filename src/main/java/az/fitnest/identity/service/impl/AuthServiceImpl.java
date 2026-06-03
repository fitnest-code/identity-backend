package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.dto.response.PasswordVerificationResultResponse;
import az.fitnest.identity.service.AuthService;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.TokenIssuanceService;
import az.fitnest.identity.service.DeviceDetectionService;
import az.fitnest.identity.util.TokenHasher;
import az.fitnest.identity.dto.request.LoginRequest;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.dto.response.LoginResult;
import az.fitnest.identity.dto.response.RefreshResponse;
import az.fitnest.identity.dto.response.OtpSendResponse;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.model.enums.SessionStatus;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.entity.AuthToken;
import az.fitnest.identity.security.JwtService;
import az.fitnest.identity.security.RedisTokenService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final AuthTokenRepository authTokenRepository;
    private final RedisTokenService redisTokenService;
    private final TokenIssuanceService tokenIssuanceService;
    private final OtpService otpService;
    private final DeviceDetectionService deviceDetectionService;
    private final TokenHasher tokenHasher;
    private final MessageSource messageSource;

    @Value("${auth.account-lock.max-failed-attempts:5}")
    private int maxFailedLoginAttempts;

    @Value("${auth.account-lock.lock-duration-minutes:30}")
    private int accountLockDurationMinutes;

    @Value("${auth.reactivation.window-days:30}")
    private int reactivationWindowDays;

    @Override
    public LoginResult login(LoginRequest request) {
        String mobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.mobile());
        AuthenticationResult result = authenticate(mobile, request.password());

        if (result.status() == AuthenticationStatus.REACTIVATION_REQUIRED) {
            az.fitnest.identity.dto.request.OtpSendRequest otpRequest = new az.fitnest.identity.dto.request.OtpSendRequest(
                    az.fitnest.identity.model.enums.OtpPurpose.REACTIVATION,
                    result.user().getMobile(),
                    null,
                    null
            );
            // Fire OTP asynchronously
            otpService.sendOtpAsync(otpRequest);

            OtpSendResponse reactivationResponse = new OtpSendResponse(
                    null, // Session ID will be in the async context or handled by callback if needed, but here we return success message
                    null,
                    null,
                    getMessage("success.otp.reactivation_sent")
            );
            return LoginResult.reactivationRequired(reactivationResponse);
        }

        String deviceType = deviceDetectionService.detectDeviceType();
        String activeJti = redisTokenService.getActiveSession(result.user().getId(), deviceType);
        if (activeJti != null) {
            redisTokenService.revokeAccessToken(activeJti);
        }

        User user = result.user();
        if (user.getSessionStatus() != SessionStatus.HAVE_SESSIONS) {
            user.setSessionStatus(SessionStatus.HAVE_SESSIONS);
            userRepository.save(user);
        }

        LoginResponse tokens = tokenIssuanceService.issueTokens(user, deviceType);
        return LoginResult.success(tokens);
    }

    @Transactional
    public AuthenticationResult authenticate(String mobile, String password) {
        User user = userRepository.findFirstByMobile(mobile)
                .orElseThrow(() -> new InvalidCredentialsException("error.auth.invalid_credentials"));

        validateUserStatus(user);

        PasswordVerificationResultResponse verification = passwordService.verifyPassword(password, user.getPasswordHash());
        if (!verification.matches()) {
            handleFailedLogin(user);
            throw new InvalidCredentialsException("error.auth.invalid_credentials");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            return new AuthenticationResult(user, AuthenticationStatus.REACTIVATION_REQUIRED);
        }

        checkPasswordAndUpgrade(user, password, verification);
        resetLockout(user);

        return new AuthenticationResult(user, AuthenticationStatus.SUCCESS);
    }

    private void validateUserStatus(User user) {
        Instant now = Instant.now();
        if (user.getStatus() == UserStatus.DELETED) {
            throw new InvalidCredentialsException("error.auth.account_deleted");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new InvalidCredentialsException("error.auth.account_blocked");
        }

        if (user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new InvalidCredentialsException("error.auth.account_locked");
        }

        if (user.getStatus() == UserStatus.INACTIVE && user.getInactiveAt() != null) {
            if (user.getInactiveAt().plus(java.time.Duration.ofDays(reactivationWindowDays)).isBefore(now)) {
                throw new InvalidCredentialsException("error.auth.account_deleted");
            }
        }
    }

    private void handleFailedLogin(User user) {
        Integer attempts = userRepository.incrementFailedLoginAttemptsAndReturn(user.getId());
        if (attempts != null && attempts >= maxFailedLoginAttempts) {
            userRepository.updateLockStatus(
                    user.getId(),
                    attempts,
                    Instant.now().plus(java.time.Duration.ofMinutes(accountLockDurationMinutes)),
                    UserStatus.LOCKED
            );
        }
    }

    private void checkPasswordAndUpgrade(User user, String password, PasswordVerificationResultResponse verification) {
        boolean updated = false;
        if (verification.upgradeRecommended()) {
            user.setPasswordHash(passwordService.hashPassword(password));
            updated = true;
        }

        if (user.getSessionStatus() == SessionStatus.NO_SESSIONS) {
            user.setSessionStatus(SessionStatus.HAVE_SESSIONS);
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
        }
    }

    private void resetLockout(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public RefreshResponse refresh(String refreshToken) {
        Long userId;
        Instant expiration;
        try {
            userId = jwtService.parseUserId(refreshToken, "refresh");
            expiration = jwtService.parseExpiration(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("error.auth.invalid_credentials");
        }

        if (expiration.isBefore(Instant.now())) {
            throw new UnauthorizedException("error.auth.invalid_credentials");
        }

        User user = internalRefresh(userId, refreshToken);
        LoginResponse tokens = tokenIssuanceService.issueTokens(user, deviceDetectionService.detectDeviceType());

        if (user.getSessionStatus() != SessionStatus.HAVE_SESSIONS) {
            user.setSessionStatus(SessionStatus.HAVE_SESSIONS);
            userRepository.save(user);
        }

        return new RefreshResponse(tokens.accessToken(), tokens.refreshToken());
    }

    private User internalRefresh(Long userId, String refreshToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("error.auth.invalid_credentials"));

        Instant now = Instant.now();
        if (user.getStatus() == UserStatus.INACTIVE || user.getStatus() == UserStatus.BLOCKED || (user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))) {
            String errorCode = user.getStatus() == UserStatus.BLOCKED ? "error.auth.account_blocked" : "error.auth.invalid_credentials";
            throw new UnauthorizedException(errorCode);
        }

        String refreshTokenHash = tokenHasher.hash(refreshToken);
        int consumed = authTokenRepository.consumeRefreshToken(userId, refreshTokenHash, now);

        if (consumed == 0) {
            throw new UnauthorizedException("error.auth.invalid_credentials");
        }

        return user;
    }

    @Override
    @Transactional
    public void logout(String accessToken) {
        try {
            Long userId = jwtService.parseUserId(accessToken);
            String jti = jwtService.parseJti(accessToken);

            redisTokenService.revokeAccessToken(jti);
            
            AuthToken token = authTokenRepository.findByJti(jti);
            String deviceType = (token != null) ? token.getDeviceType() : "UNKNOWN";

            String activeJti = redisTokenService.getActiveSession(userId, deviceType);
            if (jti.equals(activeJti)) {
                redisTokenService.removeActiveSession(userId, deviceType);
            }

            // Revoke ONLY the current session/JTI instead of all user tokens
            authTokenRepository.deleteByJti(jti);
            userRepository.markNoSessionsIfNone(userId, SessionStatus.NO_SESSIONS);
        } catch (JwtException e) {
            // Only catch JWT related issues, let others bubble up or handle specifically
            throw new UnauthorizedException("error.auth.invalid_token");
        } catch (Exception e) {
            // Ignore other errors during logout to ensure best effort
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

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    private enum AuthenticationStatus {SUCCESS, REACTIVATION_REQUIRED}

    private record AuthenticationResult(User user, AuthenticationStatus status) {
    }

}
