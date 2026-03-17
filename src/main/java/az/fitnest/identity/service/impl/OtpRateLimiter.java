package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.service.RedisKeyBuilder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OtpRateLimiter {
    static final String RATE_LIMIT_SCRIPT_STRING = """
                -- KEYS[1] = windowKey
                -- KEYS[2] = cooldownKey
                -- ARGV[1] = windowMs
                -- ARGV[2] = cooldownMs
                -- ARGV[3] = maxAttempts

                local windowKey = KEYS[1]
                local cdKey     = KEYS[2]
                local windowMs   = tonumber(ARGV[1])
                local cooldownMs = tonumber(ARGV[2])
                local maxA       = tonumber(ARGV[3])

                -- 1. Cooldown gate (cheap check)
                if cooldownMs > 0 then
                    local cdTtl = redis.call('PTTL', cdKey)
                    if cdTtl > 0 then
                        return {0, math.floor((cdTtl + 999) / 1000)} -- denied, waitSec
                    end
                end

                -- 2. Fixed window counter
                local n = redis.call('INCR', windowKey)
                if n == 1 then
                    redis.call('PEXPIRE', windowKey, windowMs)
                end

                if n > maxA then
                    local wTtl = redis.call('PTTL', windowKey)
                    if wTtl < 0 then wTtl = windowMs end -- safety fallback
                    return {0, math.floor((wTtl + 999) / 1000)} -- denied, waitSec
                end

                -- 3. Set/update cooldown
                if cooldownMs > 0 then
                    redis.call('PSETEX', cdKey, cooldownMs, '1')
                end

                return {1, 0} -- allowed
            """;
    @SuppressWarnings("rawtypes")
    static final DefaultRedisScript<java.util.List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            RATE_LIMIT_SCRIPT_STRING,
            java.util.List.class
    );
    private final StringRedisTemplate redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;
    private final OtpRateLimitProperties properties;
    private final MeterRegistry meterRegistry;
    private final PhoneNormalizer phoneNormalizer;
    private final Cache<String, Long> localBurstShield;

    public OtpRateLimiter(StringRedisTemplate redisTemplate,
                          RedisKeyBuilder redisKeyBuilder,
                          OtpRateLimitProperties properties,
                          MeterRegistry meterRegistry,
                          PhoneNormalizer phoneNormalizer) {
        this.redisTemplate = redisTemplate;
        this.redisKeyBuilder = redisKeyBuilder;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.phoneNormalizer = phoneNormalizer;
        this.localBurstShield = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(2, TimeUnit.SECONDS)
                .build();
    }

    public RateLimitResult checkRateLimit(OtpPurpose purpose, String phoneNumber, String clientIp) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OtpRateLimiter.class);
        log.info("[OtpRateLimiter] Rate limit check: purpose={}, phoneNumber={}, clientIp={}", purpose, phoneNumber, clientIp);
        if (purpose == null || phoneNumber == null || phoneNumber.isEmpty()) {
            meterRegistry.counter("otp.ratelimit.invalid.input").increment();
            log.warn("[OtpRateLimiter] Invalid input for rate limit: purpose={}, phoneNumber={}, clientIp={}", purpose, phoneNumber, clientIp);
            return new RateLimitResult(false, properties.getWindowSeconds());
        }

        if (clientIp != null) {
            String shieldKey = purpose.name() + ":" + clientIp;
            Long count = localBurstShield.getIfPresent(shieldKey);
            if (count != null && count > 5) {
                meterRegistry.counter("otp.ratelimit.local.shield.denied").increment();
                log.warn("[OtpRateLimiter] Local burst shield activated, too many requests from clientIp: {}", clientIp);
                return new RateLimitResult(false, 30);
            }
            localBurstShield.put(shieldKey, count == null ? 1L : count + 1);
        }

        String finalIdentifier;
        if (purpose == OtpPurpose.EMAIL_CHANGE) {
            finalIdentifier = phoneNumber.toLowerCase().trim();
        } else {
            finalIdentifier = phoneNormalizer.normalizeAzerbaijanPhoneNumber(phoneNumber);
            if (finalIdentifier == null) {
                meterRegistry.counter("otp.ratelimit.invalid.identifier").increment();
                log.warn("[OtpRateLimiter] Invalid identifier for rate limit: purpose={}, phoneNumber={}", purpose, phoneNumber);
                return denyDefault();
            }
        }
        RateLimitResult identifierResult = checkRedisRateLimit("identifier", purpose, finalIdentifier);
        log.info("[OtpRateLimiter] Redis rate limit result for identifier: allowed={}, waitTimeSeconds={}", identifierResult.allowed(), identifierResult.waitTimeSeconds());
        if (!identifierResult.allowed()) return identifierResult;
        if (clientIp != null) {
            RateLimitResult ipResult = checkRedisRateLimit("ip", purpose, clientIp);
            log.info("[OtpRateLimiter] Redis rate limit result for clientIp: allowed={}, waitTimeSeconds={}", ipResult.allowed(), ipResult.waitTimeSeconds());
            if (!ipResult.allowed()) return ipResult;
        }
        return new RateLimitResult(true, 0);
    }

    public RateLimitResult checkRateLimit(OtpPurpose purpose, String phoneNumber) {
        return checkRateLimit(purpose, phoneNumber, null);
    }

    private RateLimitResult checkRedisRateLimit(String dimension, OtpPurpose purpose, String identifier) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OtpRateLimiter.class);
        RedisKeyBuilder.RedisKeys keys = redisKeyBuilder.rateLimitKeys(purpose, identifier);
        long start = System.nanoTime();
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Long> res = (java.util.List<Long>) redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    java.util.List.of(keys.windowKey(), keys.cooldownKey()),
                    String.valueOf(properties.getWindowMillis()),
                    String.valueOf(properties.getCooldownMillis()),
                    String.valueOf(properties.getMaxAttempts())
            );
            meterRegistry.timer("otp.ratelimit.redis.latency", "dimension", dimension)
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            log.info("[OtpRateLimiter] Redis script result: {}", res);
            if (res == null || res.size() < 2) {
                meterRegistry.counter("otp.ratelimit.redis.bad_response").increment();
                log.warn("[OtpRateLimiter] Bad Redis response for rate limit: dimension={}, keys={}", dimension, keys);
                return denyDefault();
            }
            boolean allowed = res.get(0) == 1L;
            long waitSec = res.get(1);
            meterRegistry.counter("otp.ratelimit.result", "allowed", Boolean.toString(allowed), "dimension", dimension).increment();
            log.info("[OtpRateLimiter] Rate limit check result: allowed={}, waitSec={}, dimension={}, keys={}", allowed, waitSec, dimension, keys);
            return new RateLimitResult(allowed, waitSec);
        } catch (Exception e) {
            meterRegistry.counter("otp.ratelimit.error", "dimension", dimension).increment();
            log.error("[OtpRateLimiter] Redis error during OTP {} rate limit check: {}", dimension, keys, e);
            return properties.isFailOpen() ? new RateLimitResult(true, 0) : denyDefault();
        }
    }

    private RateLimitResult denyDefault() {
        return new RateLimitResult(false, properties.getWindowSeconds());
    }

    public OtpRateLimitProperties getProperties() {
        return properties;
    }

    public record RateLimitResult(boolean allowed, long waitTimeSeconds) {
    }
}
