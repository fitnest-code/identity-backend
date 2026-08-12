package az.fitnest.identity.security;

import az.fitnest.identity.model.enums.UserStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisTokenService {

    private final StringRedisTemplate redisTemplate;
    private final String accessPrefix;
    private final String sessionPrefix;

    public RedisTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${security.redis.access-token-prefix:auth:token:access:}") String accessPrefix,
            @Value("${security.redis.session-prefix:auth:user:session:}") String sessionPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.accessPrefix = accessPrefix;
        this.sessionPrefix = sessionPrefix;
    }

    public boolean isAccessTokenActive(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(accessKey(jti)));
    }

    public void activateAccessToken(String jti, Duration ttl) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("jti is blank.");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive.");
        }

        redisTemplate.opsForValue().set(accessKey(jti), "1", ttl);
    }

    public void revokeAccessToken(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        redisTemplate.delete(accessKey(jti));
    }

    public void setActiveSession(Long userId, String deviceType, String jti, Duration ttl) {
        redisTemplate.opsForValue().set(sessionKey(userId, deviceType), jti, ttl);
    }

    public void setActiveSession(Long userId, String jti, Duration ttl) {
        setActiveSession(userId, "web", jti, ttl);
    }

    public String getActiveSession(Long userId, String deviceType) {
        return redisTemplate.opsForValue().get(sessionKey(userId, deviceType));
    }

    public String getActiveSession(Long userId) {
        return getActiveSession(userId, "web");
    }

    public void removeActiveSession(Long userId, String deviceType) {
        redisTemplate.delete(sessionKey(userId, deviceType));
    }

    public void removeActiveSession(Long userId) {
        removeActiveSession(userId, "web");
    }

    public void addSessionToIndex(Long userId, String jti, Duration ttl) {
        String key = getSessionIndexKey(userId);
        redisTemplate.opsForSet().add(key, jti);
        redisTemplate.expire(key, ttl);
    }

    public void removeAllSessions(Long userId) {
        String key = getSessionIndexKey(userId);
        var jtIs = redisTemplate.opsForSet().members(key);
        if (jtIs != null) {
            for (String jti : jtIs) {
                revokeAccessToken(jti);
            }
        }
        redisTemplate.delete(key);
    }

    private String accessKey(String token) {
        return accessPrefix + token;
    }

    private String sessionKey(Long userId) {
        return sessionPrefix + userId;
    }

    private String sessionKey(Long userId, String deviceType) {
        String category = isMobile(deviceType) ? "mobile" : "web";
        return sessionPrefix + userId + ":" + category;
    }

    private boolean isMobile(String deviceType) {
        if (deviceType == null) {
            return false;
        }
        return "iOS".equalsIgnoreCase(deviceType) || "Android".equalsIgnoreCase(deviceType) || "mobile".equalsIgnoreCase(deviceType);
    }

    private String getSessionIndexKey(Long userId) {
        return "user_sessions:" + userId;
    }
}
