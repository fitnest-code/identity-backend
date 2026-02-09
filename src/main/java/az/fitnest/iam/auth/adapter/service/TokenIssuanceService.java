package az.fitnest.iam.auth.adapter.service;

import az.fitnest.iam.auth.adapter.persistence.AuthTokenRepository;
import az.fitnest.iam.auth.api.dto.response.LoginResponse;
import az.fitnest.iam.auth.domain.model.AuthToken;
import az.fitnest.iam.security.JwtService;
import az.fitnest.iam.security.RedisTokenService;
import az.fitnest.iam.user.api.dto.mapper.UserResponseMapper;
import az.fitnest.iam.user.api.dto.response.UserResponse;
import az.fitnest.iam.user.domain.model.User;
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
    private final az.fitnest.iam.legal.adapter.service.LegalService legalService;
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
