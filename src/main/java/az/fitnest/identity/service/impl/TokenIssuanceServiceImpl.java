package az.fitnest.identity.service.impl;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;

import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.entity.AuthToken;
import az.fitnest.identity.security.JwtService;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenIssuanceServiceImpl implements TokenIssuanceService {

    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final az.fitnest.identity.service.impl.LegalService legalService;
    private final AuthTokenRepository authTokenRepository;

        @Override
    public LoginResponse issueTokens(User user) {
        String roleName = (user.getRole() != null) ? user.getRole().getName().name() : "ROLE_USER"; 
        java.util.List<String> roles = java.util.List.of(roleName);

        String accessToken = jwtService.generateAccessToken(user.getId(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        Instant accessExpiresAt = jwtService.parseExpiration(accessToken);
        Instant refreshExpiresAt = jwtService.parseExpiration(refreshToken);

        Duration accessTtl = Duration.between(Instant.now(), accessExpiresAt);
        redisTokenService.activateAccessToken(accessToken, accessTtl);

        saveAuthToken(user.getId(), accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);

        boolean consentRequired = legalService.isConsentRequired(user.getId());
        UserResponse userResponse = UserResponseMapper.toResponse(user, consentRequired);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
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
}
