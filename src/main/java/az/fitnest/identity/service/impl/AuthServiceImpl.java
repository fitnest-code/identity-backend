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

        // Enforce single device policy: logout all previous sessions before issuing new tokens
        logoutAll(result.user().getId());

        return tokenIssuanceService.issueTokens(result.user(), DeviceDetector.detectDeviceType());
    }

    @Transactional
    public AuthenticationResult authenticate(String mobile, String password) {
        User user = userRepository.findByMobileIncludingDeleted(mobile)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.isDeleted()) {
            if (user.getPasswordHash() == null || !passwordService.verifyPassword(password, user.getPasswordHash())) {
                incrementFailedLoginAttempts(user);
                throw new InvalidCredentialsException("Invalid credentials");
            }
            return new AuthenticationResult(user, AuthenticationStatus.REACTIVATION_REQUIRED);
        }

        if (user.getStatus() == User.Status.LOCKED || isAccountLocked(user)) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (user.getPasswordHash() == null || !passwordService.verifyPassword(password, user.getPasswordHash())) {
            incrementFailedLoginAttempts(user);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (user.getStatus() == User.Status.NO_SESSIONS) {
            user.setStatus(User.Status.ACTIVE);
        }

        resetFailedLoginAttempts(user);
        userRepository.save(user);

        return new AuthenticationResult(user, AuthenticationStatus.SUCCESS);
    }

    private record AuthenticationResult(User user, AuthenticationStatus status) {}
    private enum AuthenticationStatus { SUCCESS, REACTIVATION_REQUIRED }

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

        User user = internalRefresh(userId, refreshToken);
        
        return RefreshResponse.builder()
                .accessToken(tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType()).getAccessToken())
                .refreshToken(tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType()).getRefreshToken())
                .build();
    }

    @Transactional
    public User internalRefresh(Long userId, String refreshToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.isDeleted() || user.getStatus() == User.Status.LOCKED) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String refreshTokenHash = TokenHasher.hash(refreshToken);
        az.fitnest.identity.entity.AuthToken authToken = authTokenRepository.findByRefreshTokenHash(refreshTokenHash);
        
        if (authToken == null || authToken.isRevoked() || (authToken.getRefreshExpiresAt() != null && authToken.getRefreshExpiresAt().isBefore(LocalDateTime.now()))) {
             throw new UnauthorizedException("Invalid credentials");
        }

        authTokenRepository.deleteByRefreshTokenHash(refreshTokenHash);
        return user;
    }

    @Override
    public void logout(String accessToken) {
        Long userId = jwtService.parseUserId(accessToken);
        String jti = jwtService.parseJti(accessToken);
        
        redisTokenService.revokeAccessToken(jti);
        String activeJti = redisTokenService.getActiveSession(userId);
        if (jti.equals(activeJti)) {
            redisTokenService.removeActiveSession(userId);
        }
        
        internalLogout(userId, accessToken);
    }

    @Transactional
    public void internalLogout(Long userId, String accessToken) {
        authTokenRepository.deleteByAccessTokenHash(TokenHasher.hash(accessToken));
        
        if (!authTokenRepository.existsByUserId(userId)) {
            userRepository.findById(userId).ifPresent(u -> {
                u.setStatus(User.Status.NO_SESSIONS);
                userRepository.save(u);
            });
        }
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
        userRepository.findById(userId).ifPresent(u -> {
            u.setStatus(User.Status.NO_SESSIONS);
            userRepository.save(u);
        });
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
        if (user.getStatus() == User.Status.LOCKED) {
            return true;
        }

        LocalDateTime lockedUntil = user.getLockedUntil();
        if (lockedUntil != null) {
            if (lockedUntil.isAfter(LocalDateTime.now())) {
                return true;
            } else {
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
