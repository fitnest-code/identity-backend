package az.fitnest.identity.service.impl;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;

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

    @Value("${auth.account-lock.max-failed-attempts:5}")
    private int maxFailedLoginAttempts;

    @Value("${auth.account-lock.lock-duration-minutes:30}")
    private int accountLockDurationMinutes;

    @Transactional
        @Override
    public LoginResponse login(LoginRequest request) {
        String mobile = az.fitnest.identity.criteria.MobileNumberUtils.normalize(request.getMobile());
        User user = userRepository.findByMobileIncludingDeleted(mobile)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.isDeleted()) {
            // Check if password is correct first
            if (user.getPasswordHash() == null || !passwordService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
                incrementFailedLoginAttempts(user);
                throw new InvalidCredentialsException("Invalid credentials");
            }
            
            // Trigger reactivation OTP
            az.fitnest.identity.dto.OtpSendRequest otpRequest = az.fitnest.identity.dto.OtpSendRequest.builder()
                    .mobile(user.getMobile())
                    .purpose(az.fitnest.identity.constants.OtpPurpose.REACTIVATION)
                    .build();
            az.fitnest.identity.dto.OtpSendResponse otpResponse = otpService.sendOtp(otpRequest);
            
            throw new az.fitnest.identity.exception.AccountDeactivatedException(
                    "Account is deactivated. Please verify with OTP to reactivate.",
                    otpResponse.getOtpSessionId()
            );
        }

        // Deny login if user is administratively locked
        if (user.getStatus() == User.Status.LOCKED) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // If user was marked as NO_SESSIONS (no active sessions), continue with login and mark ACTIVE
        if (user.getStatus() == User.Status.NO_SESSIONS) {
            user.setStatus(User.Status.ACTIVE);
            userRepository.save(user);
        }

        if (isAccountLocked(user)) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (user.getPasswordHash() == null) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!passwordService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            incrementFailedLoginAttempts(user);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        resetFailedLoginAttempts(user);

        // Log out from all other devices before issuing new tokens
        logoutAll(user.getId());

        return tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());
    }

    @Transactional
        @Override
    public RefreshResponse refresh(String refreshToken) {
        Long userId;
        Instant expiration;
        
        try {
            userId = jwtService.parseUserId(refreshToken);
            expiration = jwtService.parseExpiration(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (expiration.isBefore(Instant.now())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.isDeleted()) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Prevent token refresh if user is locked
        if (user.getStatus() == User.Status.LOCKED) {
            throw new UnauthorizedException("Invalid credentials");
        }

        authTokenRepository.deleteByUserId(userId);
        
        String deviceType = DeviceDetector.detectDeviceType();
        LoginResponse loginResponse = tokenIssuanceService.issueTokens(user, deviceType); 
        
        return RefreshResponse.builder()
                .accessToken(loginResponse.getAccessToken())
                .refreshToken(loginResponse.getRefreshToken())
                .build();
    }

    @Transactional
    @Override
    public void logout(String accessToken) {
        Long userId = jwtService.parseUserId(accessToken);
        String jti = jwtService.parseJti(accessToken);
        redisTokenService.revokeAccessToken(accessToken);
        String activeJti = redisTokenService.getActiveSession(userId);
        if (jti.equals(activeJti)) {
            redisTokenService.removeActiveSession(userId);
        }
        // Only delete the current token
        authTokenRepository.deleteByAccessToken(accessToken);
        // If user has no more tokens, set status to NO_SESSIONS
        if (authTokenRepository.findByUserId(userId).isEmpty()) {
            userRepository.findById(userId).ifPresent(u -> {
                u.setStatus(User.Status.NO_SESSIONS);
                userRepository.save(u);
            });
        }
    }

    @Transactional
    @Override
    public void logoutAll(Long userId) {
        // Revoke all access tokens in Redis
        var tokens = authTokenRepository.findByUserId(userId);
        for (var token : tokens) {
            redisTokenService.revokeAccessToken(token.getAccessToken());
        }
        // Remove session key in Redis
        redisTokenService.removeActiveSession(userId);
        // Remove all tokens from DB
        authTokenRepository.deleteByUserId(userId);
        // Mark user as having no active sessions
        userRepository.findById(userId).ifPresent(u -> {
            u.setStatus(User.Status.NO_SESSIONS);
            userRepository.save(u);
        });
        // Optionally: Remove OTP session pointers if needed (not implemented here)
    }

    private void incrementFailedLoginAttempts(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedLoginAttempts) {
            user.setAccountLocked(true);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(accountLockDurationMinutes));
            user.setStatus(User.Status.LOCKED);
        }

        userRepository.save(user);
    }

    private void resetFailedLoginAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    private boolean isAccountLocked(User user) {
        if (user.isAccountLocked()) {
            return true;
        }

        if (user.isAccountLocked()) {
            LocalDateTime lockedUntil = user.getLockedUntil();
            if (lockedUntil == null || !lockedUntil.isAfter(LocalDateTime.now())) {
                unlockAccount(user);
            }
        }

        return false;
    }

    private void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        user.setStatus(User.Status.ACTIVE);
        userRepository.save(user);
    }

}
