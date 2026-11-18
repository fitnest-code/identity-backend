package az.fitnest.identity.security;

import az.fitnest.identity.model.enums.UserStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.DataType;
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

    private final long sessionGraceSeconds;

    public RedisTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${security.redis.access-token-prefix:auth:token:access:}") String accessPrefix,
            @Value("${security.redis.session-prefix:auth:user:session:}") String sessionPrefix,
            @Value("${security.redis.session-grace-seconds:45}") long sessionGraceSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.accessPrefix = accessPrefix;
        this.sessionPrefix = sessionPrefix;
        this.sessionGraceSeconds = Math.max(0, sessionGraceSeconds);
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
     * Score = expiry timestamp (epoch seconds).
     *
     * Rotation keeps the previous JTI(s) alive for {@code sessionGraceSeconds}: the old member's score is
     * lowered to now+grace instead of deleting the key. Replacing the ZSET outright made every API call
     * still in flight with the previous access token fail with SESSION_NOT_FOUND, which triggered a
     * refresh storm across admin tabs. The grace window is bounded and the new JTI always has the highest
     * score, so there is still one logical session per category (getActiveSession returns the newest).
     * The gateway only checks "score > now" for the presented JTI, so no gateway change is required.
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

        // Legacy string-format keys would raise WRONGTYPE on ZSET ops; drop them (no grace for the legacy value).
        DataType existingType = redisTemplate.type(key);
        if (existingType != null && existingType != DataType.NONE && existingType != DataType.ZSET) {
            redisTemplate.delete(key);
        }

        long now = Instant.now().getEpochSecond();
        long graceExpiry = now + sessionGraceSeconds;
        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
        zset.removeRangeByScore(key, 0, now);
        Set<ZSetOperations.TypedTuple<String>> previous = zset.rangeWithScores(key, 0, -1);
        if (previous != null) {
            for (ZSetOperations.TypedTuple<String> entry : previous) {
                String previousJti = entry.getValue();
                Double score = entry.getScore();
                if (previousJti == null || previousJti.equals(jti)) {
                    continue;
                }
                // Only ever shorten: a member already inside its grace window keeps its earlier deadline.
                if (score != null && score > graceExpiry) {
                    zset.add(key, previousJti, graceExpiry);
                }
            }
        }

        // Add new JTI to ZSET with expiry score
        zset.add(key, jti, expiryScore);

        // Set TTL on the key itself (slightly longer than max token TTL for safety)
        redisTemplate.expire(key, ttl.plus(Duration.ofMinutes(5)));
    }

    public void setActiveSession(Long userId, String jti, Duration ttl) {
        setActiveSession(userId, "web", jti, ttl);
    }

    /**
     * Returns the most recent valid JTI for the user/device category.
     * Removes expired entries (score < now) before returning.
     * Handles migration from old string keys to ZSET.
     */
    public String getActiveSession(Long userId, String deviceType) {
        String key = sessionKey(userId, deviceType);
        long now = Instant.now().getEpochSecond();

        try {
            // Remove expired entries (score < now)
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);

            // Get the JTI with highest score (most recent)
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 0);
            if (tuples != null && !tuples.isEmpty()) {
                return tuples.iterator().next().getValue();
            }
        } catch (Exception e) {
            // Fallback: if key exists as string (old format), read it and migrate
            String oldValue = redisTemplate.opsForValue().get(key);
            if (oldValue != null && !oldValue.isBlank()) {
                // Migrate: delete string, add as ZSET with current time + 30s grace
                redisTemplate.delete(key);
                long graceExpiry = Instant.now().plusSeconds(30).getEpochSecond();
                redisTemplate.opsForZSet().add(key, oldValue, graceExpiry);
                redisTemplate.expire(key, Duration.ofMinutes(5));
                return oldValue;
            }
        }
        return null;
    }

    public String getActiveSession(Long userId) {
        return getActiveSession(userId, "web");
    }

    /**
     * Checks if a specific JTI is currently valid for the user/device category.
     * This is the method the gateway should use for session validation.
     * Handles migration from old string keys to ZSET.
     */
    public boolean isSessionValid(Long userId, String deviceType, String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        String key = sessionKey(userId, deviceType);
        long now = Instant.now().getEpochSecond();

        try {
            // Clean expired entries
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);

            // Check if this JTI exists with score > now
            Double score = redisTemplate.opsForZSet().score(key, jti);
            return score != null && score > now;
        } catch (Exception e) {
            // Fallback: if key exists as string (old format), check equality
            String oldValue = redisTemplate.opsForValue().get(key);
            if (oldValue != null && oldValue.equals(jti)) {
                // Migrate to ZSET with grace period
                redisTemplate.delete(key);
                long graceExpiry = Instant.now().plusSeconds(30).getEpochSecond();
                redisTemplate.opsForZSet().add(key, jti, graceExpiry);
                redisTemplate.expire(key, Duration.ofMinutes(5));
                return true;
            }
            return false;
        }
    }

    /** Removes one JTI (e.g. explicit logout of that token) without touching other members in its grace window. */
    public void removeSessionMember(Long userId, String deviceType, String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        try {
            redisTemplate.opsForZSet().remove(sessionKey(userId, deviceType), jti);
        } catch (Exception ignored) {
            // Legacy string key: nothing member-level to remove.
        }
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