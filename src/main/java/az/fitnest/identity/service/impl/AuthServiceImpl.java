package az.fitnest.identity.service.impl;

import az.fitnest.identity.service.*;
import az.fitnest.identity.util.TokenHasher;

import az.fitnest.identity.dto.LoginRequest;
import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.dto.RefreshResponse;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.entity.User;
import az.fitnest.identity.security.JwtService;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.util.DeviceDetector;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

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
    private final TokenHasher tokenHasher;

    @Value("${auth.account-lock.max-failed-attempts:5}")
    private int maxFailedLoginAttempts;

    @Value("${auth.account-lock.lock-duration-minutes:30}")
    private int accountLockDurationMinutes;

    @Override
    public LoginResponse login(LoginRequest request) {
        String mobile = az.fitnest.identity.criteria.MobileNumberUtils.normalize(request.getMobile());
        AuthenticationResult result = authenticate(mobile, request.getPassword());

        if (result.status() == AuthenticationStatus.REACTIVATION_REQUIRED) {
            az.fitnest.identity.dto.OtpSendRequest otpRequest = az.fitnest.identity.dto.OtpSendRequest.builder()
                    .mobile(result.user().getMobile())
                    .purpose(az.fitnest.identity.constants.OtpPurpose.REACTIVATION)
                    .build();
            az.fitnest.identity.dto.OtpSendResponse otpResponse = otpService.sendOtp(otpRequest);
            throw new az.fitnest.identity.exception.AccountDeactivatedException(
                    "Account is deactivated. Please verify with OTP to reactivate.",
                    otpResponse.getOtpSessionId()
            );
        }

        // Single device policy: revoke only the active session in Redis to avoid heavy DB churn. 
        // Token records in DB are kept for auditability.
        String activeJti = redisTokenService.getActiveSession(result.user().getId());
        if (activeJti != null) {
            redisTokenService.revokeAccessToken(activeJti);
            redisTokenService.removeActiveSession(result.user().getId());
        }

        return tokenIssuanceService.issueTokens(result.user(), DeviceDetector.detectDeviceType());
    }

    @Transactional
    public AuthenticationResult authenticate(String mobile, String password) {
        User user = userRepository.findFirstByMobile(mobile)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        Instant now = Instant.now();

        if (user.isDeleted()) {
            if (user.getPasswordHash() == null || !passwordService.verifyPassword(password, user.getPasswordHash())) {
                userRepository.incrementFailedLoginAttempts(user.getId());
                throw new InvalidCredentialsException("Invalid credentials");
            }
            return new AuthenticationResult(user, AuthenticationStatus.REACTIVATION_REQUIRED);
        }

        if (isAccountLocked(user, now)) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (user.getPasswordHash() == null || !passwordService.verifyPassword(password, user.getPasswordHash())) {
            incrementFailedLoginAttempts(user.getId(), user.getFailedLoginAttempts(), now);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (user.getStatus() == User.Status.NO_SESSIONS) {
            user.setStatus(User.Status.ACTIVE);
            userRepository.save(user);
        }

        resetFailedLoginAttempts(user.getId());
        // No need for userRepository.save(user) if only status was changed, 
        // but let's be safe if it was NO_SESSIONS -> ACTIVE
        if (user.getStatus() == User.Status.ACTIVE && user.getFailedLoginAttempts() > 0) {
             // already handled by resetFailedLoginAttempts(userId) which is atomic
        }
        
        return new AuthenticationResult(user, AuthenticationStatus.SUCCESS);
    }

    private record AuthenticationResult(User user, AuthenticationStatus status) {}
    private enum AuthenticationStatus { SUCCESS, REACTIVATION_REQUIRED }

    @Override
    @Transactional
    public RefreshResponse refresh(String refreshToken) {
        Long userId;
        Instant expiration;
        try {
            userId = jwtService.parseUserId(refreshToken, "refresh");
            expiration = jwtService.parseExpiration(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (expiration.isBefore(Instant.now())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        User user = internalRefresh(userId, refreshToken);
        
        LoginResponse tokens = tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());
        
        return RefreshResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .build();
    }

    private User internalRefresh(Long userId, String refreshToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        Instant now = Instant.now();
        if (user.isDeleted() || user.isAccountLocked()) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String refreshTokenHash = tokenHasher.hash(refreshToken);
        int consumed = authTokenRepository.consumeRefreshToken(userId, refreshTokenHash, now);
        
        if (consumed == 0) {
             throw new UnauthorizedException("Invalid credentials");
        }

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
            // Handle parsing errors gracefully (token might be malformed or expired already)
            // Logout should be idempotent.
        }
    }

    @Transactional
    public void internalLogout(Long userId, String accessToken) {
        try {
            authTokenRepository.deleteByAccessTokenHash(tokenHasher.hash(accessToken));
        } catch (Exception e) {
             // Ignore if hash fails
        }
        
        userRepository.markNoSessionsIfNone(userId, User.Status.NO_SESSIONS);
    }

    @Override
    public void logoutAll(Long userId) {
        String activeJti = redisTokenService.getActiveSession(userId);
        if (activeJti != null) {
            redisTokenService.revokeAccessToken(activeJti);
        }
        redisTokenService.removeActiveSession(userId);
        internalLogoutAll(userId);
    }

    @Transactional
    public void internalLogoutAll(Long userId) {
        authTokenRepository.deleteByUserId(userId);
        userRepository.updateLockStatus(userId, 0, null, User.Status.NO_SESSIONS);
    }

    private void incrementFailedLoginAttempts(Long userId, int currentAttempts, Instant now) {
        int attempts = currentAttempts + 1;
        
        if (attempts >= maxFailedLoginAttempts) {
            userRepository.updateLockStatus(
                userId, 
                attempts, 
                now.plus(java.time.Duration.ofMinutes(accountLockDurationMinutes)), 
                User.Status.LOCKED
            );
        } else {
            userRepository.incrementFailedLoginAttempts(userId);
        }
    }

    private void resetFailedLoginAttempts(Long userId) {
        userRepository.updateLockStatus(userId, 0, null, User.Status.ACTIVE);
    }

    private boolean isAccountLocked(User user, Instant now) {
        return user.isAccountLocked();
    }

}
