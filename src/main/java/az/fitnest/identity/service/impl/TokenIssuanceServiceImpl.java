package az.fitnest.identity.service.impl;

import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.entity.AuthToken;
import az.fitnest.identity.entity.User;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.security.JwtService;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.LegalService;
import az.fitnest.identity.service.TokenIssuanceService;
import az.fitnest.identity.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenIssuanceServiceImpl implements TokenIssuanceService {

    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final LegalService legalService;
    private final AuthTokenRepository authTokenRepository;
    private final TokenHasher tokenHasher;

    @Override
    public LoginResponse issueTokens(User user, String deviceType) {
        String roleName = (user.getRole() != null) ? user.getRole().getName().name() : "ROLE_USER";
        List<String> roles = List.of(roleName);

        String accessToken = jwtService.generateAccessToken(user.getId(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        Instant accessExpiresAt = jwtService.parseExpiration(accessToken);
        Instant refreshExpiresAt = jwtService.parseExpiration(refreshToken);

        Duration accessTtl = Duration.between(Instant.now(), accessExpiresAt);
        String jti = jwtService.parseJti(accessToken);
        redisTokenService.activateAccessToken(jti, accessTtl);
        
        redisTokenService.setActiveSession(user.getId(), jti, Duration.between(Instant.now(), refreshExpiresAt));

        // Do NOT remove all previous tokens for this user. Allow multiple sessions/devices.
        saveAuthToken(user.getId(), accessToken, refreshToken, jti, deviceType, accessExpiresAt, refreshExpiresAt);

        boolean consentRequired = legalService.isConsentRequired(user.getId());
        UserResponse userResponse = UserResponseMapper.toResponse(user, consentRequired);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }

    private void saveAuthToken(Long userId, String accessToken, String refreshToken, String jti, String deviceType,
                               Instant accessExpiresAt, Instant refreshExpiresAt) {
        AuthToken authToken = AuthToken.builder()
                .userId(userId)
                .accessTokenHash(tokenHasher.hash(accessToken))
                .refreshTokenHash(tokenHasher.hash(refreshToken))
                .jti(jti)
                .deviceType(deviceType)
                .accessExpiresAt(accessExpiresAt)
                .refreshExpiresAt(refreshExpiresAt)
                .revoked(false)
                .build();

        authTokenRepository.save(authToken);
    }
}
