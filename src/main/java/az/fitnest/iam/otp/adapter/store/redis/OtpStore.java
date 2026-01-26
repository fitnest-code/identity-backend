package az.fitnest.iam.otp.adapter.store.redis;

import az.fitnest.iam.otp.domain.model.OtpSessionPayload;
import az.fitnest.iam.otp.domain.enums.OtpPurpose;
import az.fitnest.iam.shared.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OtpStore {

    private final RedisKeyBuilder redisKeyBuilder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public boolean isCooldownActive(OtpPurpose purpose, String email) {
        return redisTemplate.hasKey(redisKeyBuilder.cooldownKey(purpose, email));
    }

    public Duration getCooldownRemaining(OtpPurpose purpose, String email) {
        long sec = redisTemplate.getExpire(
                redisKeyBuilder.cooldownKey(purpose, email),
                TimeUnit.SECONDS
        );

        if (sec < 0) {
            return Duration.ZERO;
        }

        return Duration.ofSeconds(sec);
    }

    public void startCooldown(OtpPurpose purpose, String email, long cooldownSeconds) {
        redisTemplate.opsForValue().set(
                redisKeyBuilder.cooldownKey(purpose, email),
                "1",
                cooldownSeconds,
                TimeUnit.SECONDS
        );
    }

    public void saveOtpSession(String sessionId, OtpSessionPayload payload, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(
                    redisKeyBuilder.sessionKey(sessionId),
                    json,
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to serialize OTP session");
        }
    }

    public Optional<OtpSessionPayload> getOtpSession(String sessionId) {
        String json = redisTemplate.opsForValue().get(
                redisKeyBuilder.sessionKey(sessionId)
        );

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, OtpSessionPayload.class));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to deserialize OTP session");
        }
    }

    public long getOtpSessionTtlSeconds(String sessionId) {
        long sec = redisTemplate.getExpire(
                redisKeyBuilder.sessionKey(sessionId),
                TimeUnit.SECONDS
        );

        if (sec < 0) {
            return 0L;
        }

        return sec;
    }

    public void updateOtpSession(String sessionId, OtpSessionPayload payload) {
        long ttlSeconds = getOtpSessionTtlSeconds(sessionId);

        if (ttlSeconds <= 0) {
            throw new BadRequestException("OTP session not found or expired");
        }

        saveOtpSession(sessionId, payload, ttlSeconds);
    }

    public void setActiveSessionPointer(OtpPurpose purpose, String email, String sessionId, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                redisKeyBuilder.activeSessionKey(purpose, email),
                sessionId,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    public Optional<String> getActiveSessionPointer(OtpPurpose purpose, String email) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(
                        redisKeyBuilder.activeSessionKey(purpose, email)
                )
        );
    }

    public void deleteActivePointer(OtpPurpose purpose, String email) {
        redisTemplate.delete(redisKeyBuilder.activeSessionKey(purpose, email));
    }

    public void deleteSession(String sessionId) {
        redisTemplate.delete(redisKeyBuilder.sessionKey(sessionId));
    }
}
