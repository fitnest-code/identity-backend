package az.fitnest.iam.otp.adapter.store.redis;

import az.fitnest.iam.otp.domain.enums.OtpPurpose;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class OtpRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;

    @Value("${otp.rate-limit.max-attempts}")
    private int maxAttempts;

    @Value("${otp.rate-limit.window-minutes}")
    @Getter
    private int windowMinutes;

    @Value("${otp.rate-limit.cooldown-seconds}")
    private int cooldownSeconds;

    public OtpRateLimiter(StringRedisTemplate redisTemplate, RedisKeyBuilder redisKeyBuilder) {
        this.redisTemplate = redisTemplate;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    private static final String RATE_LIMIT_SCRIPT_STRING =
            "local attempts_key = KEYS[1] " +
            "local last_attempt_key = KEYS[2] " +
            "local current_time = tonumber(ARGV[1]) " +
            "local window_millis = tonumber(ARGV[2]) " +
            "local cooldown_millis = tonumber(ARGV[3]) " +
            "local max_attempts = tonumber(ARGV[4]) " +
            "local window_seconds = tonumber(ARGV[5]) " +
            "local window_ago = current_time - window_millis " +
            "local cooldown_ago = current_time - cooldown_millis " +
            "redis.call('ZREMRANGEBYSCORE', attempts_key, 0, window_ago) " +
            "redis.call('EXPIRE', attempts_key, window_seconds) " +
            "local last_attempt = redis.call('GET', last_attempt_key) " +
            "if last_attempt and tonumber(last_attempt) > cooldown_ago then " +
            "    local wait_time = math.ceil((tonumber(last_attempt) + cooldown_millis - current_time) / 1000) " +
            "    return {0, wait_time} " +
            "end " +
            "local attempt_count = redis.call('ZCARD', attempts_key) " +
            "if attempt_count >= max_attempts then " +
            "    local oldest = redis.call('ZRANGE', attempts_key, 0, 0, 'WITHSCORES') " +
            "    if oldest and #oldest >= 2 then " +
            "        local oldest_ts = tonumber(oldest[2]) " +
            "        local wait_time = math.ceil((oldest_ts + window_millis - current_time) / 1000) " +
            "        if wait_time < 1 then wait_time = 1 end " +
            "        return {0, wait_time} " +
            "    else " +
            "        return {0, window_seconds} " +
            "    end " +
            "end " +
            "redis.call('ZADD', attempts_key, current_time, tostring(current_time)) " +
            "redis.call('SETEX', last_attempt_key, window_seconds, tostring(current_time)) " +
            "return {1, 0}";

    private static final DefaultRedisScript<List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            RATE_LIMIT_SCRIPT_STRING,
            List.class
    );

    public static class RateLimitResult {
        private final boolean allowed;
        private final long waitTimeSeconds;

        public RateLimitResult(boolean allowed, long waitTimeSeconds) {
            this.allowed = allowed;
            this.waitTimeSeconds = waitTimeSeconds;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getWaitTimeSeconds() {
            return waitTimeSeconds;
        }
    }

    public RateLimitResult checkRateLimit(OtpPurpose purpose, String email) {
        String attemptsKey = redisKeyBuilder.rateLimitAttemptsKey(purpose, email);
        String lastAttemptKey = redisKeyBuilder.rateLimitLastAttemptKey(purpose, email);

        long currentTime = System.currentTimeMillis();
        long windowMillis = windowMinutes * 60L * 1000L;
        long cooldownMillis = cooldownSeconds * 1000L;
        long windowSeconds = windowMinutes * 60L;

        List<String> keys = Arrays.asList(attemptsKey, lastAttemptKey);
        List<String> args = Arrays.asList(
                String.valueOf(currentTime),
                String.valueOf(windowMillis),
                String.valueOf(cooldownMillis),
                String.valueOf(maxAttempts),
                String.valueOf(windowSeconds)
        );

        List<?> result;
        try {
            result = redisTemplate.execute(RATE_LIMIT_SCRIPT, keys, args.toArray());
        } catch (Exception e) {
            throw new az.fitnest.iam.shared.exception.InternalServerException(
                "Failed to execute Redis script for checking OTP rate limit: " + e.getMessage()
            );
        }

        if (result != null && result.size() >= 2) {
            long allowed = convertToLong(result.get(0));
            long waitTime = convertToLong(result.get(1));
            return new RateLimitResult(allowed == 1L, waitTime);
        }

        return new RateLimitResult(false, windowSeconds);
    }

    private long convertToLong(Object value) {
        if (value == null) {
            return 0L;
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        if (value instanceof byte[]) {
            try {
                return Long.parseLong(new String((byte[]) value));
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
