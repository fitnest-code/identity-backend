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

    public void setActiveSession(Long userId, String jti, String deviceType, Duration ttl) {
        redisTemplate.opsForValue().set(sessionKey(userId, deviceType), jti, ttl);
    }

    public String getActiveSession(Long userId, String deviceType) {
        return redisTemplate.opsForValue().get(sessionKey(userId, deviceType));
    }

    public void removeActiveSession(Long userId, String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        String webKey = sessionKey(userId, "web");
        String mobileKey = sessionKey(userId, "mobile");
        if (jti.equals(redisTemplate.opsForValue().get(webKey))) {
            redisTemplate.delete(webKey);
        } else if (jti.equals(redisTemplate.opsForValue().get(mobileKey))) {
            redisTemplate.delete(mobileKey);
        }
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
        redisTemplate.delete(sessionKey(userId, "web"));
        redisTemplate.delete(sessionKey(userId, "mobile"));
    }

    private String accessKey(String token) {
        return accessPrefix + token;
    }

    private String getDeviceCategory(String deviceType) {
        if (deviceType == null) {
            return "web";
        }
        String dt = deviceType.toLowerCase().trim();
        if (dt.contains("ios") || dt.contains("android")) {
            return "mobile";
        }
        return "web";
    }

    private String sessionKey(Long userId, String deviceType) {
        return sessionPrefix + userId + ":" + getDeviceCategory(deviceType);
    }

    private String getSessionIndexKey(Long userId) {
        return "user_sessions:" + userId;
    }
}
