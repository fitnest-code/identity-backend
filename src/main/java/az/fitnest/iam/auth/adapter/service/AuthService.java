package az.fitnest.iam.auth.adapter.service;

import az.fitnest.iam.auth.api.dto.request.LoginRequest;
import az.fitnest.iam.auth.api.dto.request.RegisterCompleteRequest;
import az.fitnest.iam.auth.api.dto.response.LoginResponse;
import az.fitnest.iam.auth.adapter.persistence.AuthTokenRepository;
import az.fitnest.iam.auth.domain.model.AuthToken;
import az.fitnest.iam.shared.exception.InvalidCredentialsException;
import az.fitnest.iam.shared.exception.UnauthorizedException;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.domain.model.User;
import az.fitnest.iam.user.api.dto.response.UserResponse;
import az.fitnest.iam.user.adapter.service.UserService;
import az.fitnest.iam.security.JwtService;
import az.fitnest.iam.security.RedisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final AuthTokenRepository authTokenRepository;
    private final RegistrationTokenService registrationTokenService;
    private final UserService userService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByMobile(request.getMobile())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.getAccountLocked()) {
            throw new InvalidCredentialsException("Account is locked");
        }

        if (!passwordService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            incrementFailedLoginAttempts(user);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        resetFailedLoginAttempts(user);

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        Instant accessExpiresAt = jwtService.parseExpiration(accessToken);
        Instant refreshExpiresAt = jwtService.parseExpiration(refreshToken);

        Duration accessTtl = Duration.between(Instant.now(), accessExpiresAt);
        redisTokenService.activateAccessToken(accessToken, accessTtl);

        saveAuthToken(user.getId(), accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);

        UserResponse userResponse = UserResponse.builder()
                .userId(String.valueOf(user.getId()))
                .fullName(user.getFullName())
                .mobile(user.getMobile())
                .email(user.getEmail())
                .hasAccount(user.getHasAccount())
                .setupRequired(user.getSetupRequired())
                .language(user.getLanguage() != null ? user.getLanguage().name() : null)
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        try {
            Long userId = jwtService.parseUserId(refreshToken);
            Instant expiration = jwtService.parseExpiration(refreshToken);

            if (expiration.isBefore(Instant.now())) {
                throw new UnauthorizedException("Refresh token expired");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            if (user.getAccountLocked()) {
                throw new UnauthorizedException("Account is locked");
            }

            String newAccessToken = jwtService.generateAccessToken(userId);
            String newRefreshToken = jwtService.generateRefreshToken(userId);

            Instant accessExpiresAt = jwtService.parseExpiration(newAccessToken);
            Instant refreshExpiresAt = jwtService.parseExpiration(newRefreshToken);

            Duration accessTtl = Duration.between(Instant.now(), accessExpiresAt);
            redisTokenService.activateAccessToken(newAccessToken, accessTtl);

            authTokenRepository.deleteByUserId(userId);
            saveAuthToken(userId, newAccessToken, newRefreshToken, accessExpiresAt, refreshExpiresAt);

            UserResponse userResponse = UserResponse.builder()
                    .userId(String.valueOf(user.getId()))
                    .fullName(user.getFullName())
                    .mobile(user.getMobile())
                    .email(user.getEmail())
                    .hasAccount(user.getHasAccount())
                    .setupRequired(user.getSetupRequired())
                    .language(user.getLanguage() != null ? user.getLanguage().name() : null)
                    .build();

            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .user(userResponse)
                    .build();
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }

    @Transactional
    public LoginResponse loginAfterRegistration(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!passwordService.verifyPassword(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Password verification failed");
        }

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        Instant accessExpiresAt = jwtService.parseExpiration(accessToken);
        Instant refreshExpiresAt = jwtService.parseExpiration(refreshToken);

        Duration accessTtl = Duration.between(Instant.now(), accessExpiresAt);
        redisTokenService.activateAccessToken(accessToken, accessTtl);

        saveAuthToken(user.getId(), accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);

        UserResponse userResponse = UserResponse.builder()
                .userId(String.valueOf(user.getId()))
                .fullName(user.getFullName())
                .mobile(user.getMobile())
                .email(user.getEmail())
                .hasAccount(user.getHasAccount())
                .setupRequired(user.getSetupRequired())
                .language(user.getLanguage() != null ? user.getLanguage().name() : null)
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }

    private void incrementFailedLoginAttempts(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= 5) {
            user.setAccountLocked(true);
            user.setLockedUntil(java.time.LocalDateTime.now().plusHours(1));
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

    private void saveAuthToken(Long userId, String accessToken, String refreshToken,
                               Instant accessExpiresAt, Instant refreshExpiresAt) {
        AuthToken authToken = AuthToken.builder()
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresAt(LocalDateTime.ofInstant(accessExpiresAt, java.time.ZoneId.systemDefault()))
                .refreshExpiresAt(LocalDateTime.ofInstant(refreshExpiresAt, java.time.ZoneId.systemDefault()))
                .revoked(false)
                .build();

        authTokenRepository.save(authToken);
    }

    @Transactional
    public LoginResponse completeRegistration(String registrationToken, RegisterCompleteRequest request) {
        String email = registrationTokenService.requireEmail(registrationToken);
        
        String passwordHash = passwordService.hashPassword(request.getPassword());
        userService.createNewUser(email, request.getFullName(), passwordHash);
        registrationTokenService.consume(registrationToken);
        
        return loginAfterRegistration(email, request.getPassword());
    }
}
