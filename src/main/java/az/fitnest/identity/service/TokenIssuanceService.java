package az.fitnest.identity.service;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.model.entity.AuthToken;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.security.JwtService;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

public interface TokenIssuanceService {
    LoginResponse issueTokens(User user, String deviceType);
}
