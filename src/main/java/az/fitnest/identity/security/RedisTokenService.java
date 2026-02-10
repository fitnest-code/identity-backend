package az.fitnest.identity.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisTokenService {

    private final StringRedisTemplate redisTemplate;
    private final String accessPrefix;

    public RedisTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${security.redis.access-token-prefix:auth:token:access:}") String accessPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.accessPrefix = accessPrefix;
    }

    public boolean isAccessTokenActive(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(accessKey(accessToken)));
    }

    public void activateAccessToken(String accessToken, Duration ttl) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken is blank.");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive.");
        }

        redisTemplate.opsForValue().set(accessKey(accessToken), "1", ttl);
    }

    public void revokeAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        redisTemplate.delete(accessKey(accessToken));
    }

    private String accessKey(String token) {
        return accessPrefix + token;
    }
}
