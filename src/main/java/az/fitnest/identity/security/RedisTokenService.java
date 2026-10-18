package az.fitnest.identity.security;

import az.fitnest.identity.model.enums.UserStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

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

    /**
     * Adds a JTI to the active sessions ZSET for the user/device category.
     * Score = expiry timestamp (epoch seconds). Multiple JTIs can coexist during grace period.
     * Old JTIs naturally expire when their score < current time.
     */
    public void setActiveSession(Long userId, String deviceType, String jti, Duration ttl) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive.");
        }

        String key = sessionKey(userId, deviceType);
        long expiryScore = Instant.now().plus(ttl).getEpochSecond();

        // Add new JTI to ZSET with expiry score
        redisTemplate.opsForZSet().add(key, jti, expiryScore);

        // Set TTL on the key itself (slightly longer than max token TTL for safety)
        redisTemplate.expire(key, ttl.plus(Duration.ofMinutes(5)));
    }

    public void setActiveSession(Long userId, String jti, Duration ttl) {
        setActiveSession(userId, "web", jti, ttl);
    }

    /**
     * Returns the most recent valid JTI for the user/device category.
     * Removes expired entries (score < now) before returning.
     */
    public String getActiveSession(Long userId, String deviceType) {
        String key = sessionKey(userId, deviceType);
        long now = Instant.now().getEpochSecond();

        // Remove expired entries (score < now)
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);

        // Get the JTI with highest score (most recent)
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 0);
        if (tuples != null && !tuples.isEmpty()) {
            return tuples.iterator().next().getValue();
        }
        return null;
    }

    public String getActiveSession(Long userId) {
        return getActiveSession(userId, "web");
    }

    /**
     * Checks if a specific JTI is currently valid for the user/device category.
     * This is the method the gateway should use for session validation.
     */
    public boolean isSessionValid(Long userId, String deviceType, String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        String key = sessionKey(userId, deviceType);
        long now = Instant.now().getEpochSecond();

        // Clean expired entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);

        // Check if this JTI exists with score > now
        Double score = redisTemplate.opsForZSet().score(key, jti);
        return score != null && score > now;
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
        // Also remove session ZSETs for both categories
        redisTemplate.delete(sessionKey(userId, "mobile"));
        redisTemplate.delete(sessionKey(userId, "web"));
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