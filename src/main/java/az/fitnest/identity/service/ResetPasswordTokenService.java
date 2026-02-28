package az.fitnest.identity.service;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.service.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

public interface ResetPasswordTokenService {
    String issueForIdentifier(String identifier);
    String requireIdentifier(String token);
    void consume(String token);
}
