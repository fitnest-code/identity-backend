package az.fitnest.identity.service;

import az.fitnest.identity.dto.LoginRequest;
import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.dto.RefreshResponse;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.shared.messaging.EmailService;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.entity.User;
import az.fitnest.identity.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final AuthTokenRepository authTokenRepository;
    private final TokenIssuanceService tokenIssuanceService;
    private final EmailService emailService;

    @Value("${auth.account-lock.max-failed-attempts:5}")
    private int maxFailedLoginAttempts;

    @Value("${auth.account-lock.lock-duration-minutes:30}")
    private int accountLockDurationMinutes;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String mobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.getMobile());
        User user = userRepository.findByMobileIncludingDeleted(mobile)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.isDeleted()) {
            // Auto-recover account on successful login attempt
            user.setDeleted(false);
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

        return tokenIssuanceService.issueTokens(user);
    }

    @Transactional
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

        if (isAccountLocked(user)) {
            throw new UnauthorizedException("Invalid credentials");
        }

        authTokenRepository.deleteByUserId(userId);
        
        LoginResponse loginResponse = tokenIssuanceService.issueTokens(user);
        
        return RefreshResponse.builder()
                .accessToken(loginResponse.getAccessToken())
                .refreshToken(loginResponse.getRefreshToken())
                .build();
    }

    private void incrementFailedLoginAttempts(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedLoginAttempts) {
            user.setAccountLocked(true);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(accountLockDurationMinutes));
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
        userRepository.save(user);
    }

}
