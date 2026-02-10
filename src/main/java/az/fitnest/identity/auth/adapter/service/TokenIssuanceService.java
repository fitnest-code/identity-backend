package az.fitnest.identity.auth.adapter.service;

import az.fitnest.identity.auth.adapter.persistence.AuthTokenRepository;
import az.fitnest.identity.auth.api.dto.response.LoginResponse;
import az.fitnest.identity.auth.domain.model.AuthToken;
import az.fitnest.identity.security.JwtService;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.user.api.dto.mapper.UserResponseMapper;
import az.fitnest.identity.user.api.dto.response.UserResponse;
import az.fitnest.identity.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenIssuanceService {

    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final az.fitnest.identity.legal.adapter.service.LegalService legalService;
    private final AuthTokenRepository authTokenRepository;

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
