package az.fitnest.iamservice.service.impl;

import az.fitnest.iamservice.dto.common.OtpSessionPayload;
import az.fitnest.iamservice.enums.OtpPurpose;
import az.fitnest.iamservice.exception.MethodArgumentNotValidException;
import az.fitnest.iamservice.service.RedisOtpStoreService;
import az.fitnest.iamservice.util.helper.RedisKeyHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisOtpStoreServiceImpl implements RedisOtpStoreService {

    private final RedisKeyHelper redisKeyHelper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean isCooldownActive(OtpPurpose purpose, String email) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(redisKeyHelper.cooldownKey(purpose, email)));
    }

    @Override
    public Duration getCooldownRemaining(OtpPurpose purpose, String email) {
        Long sec = redisTemplate.getExpire(
                redisKeyHelper.cooldownKey(purpose, email), TimeUnit.SECONDS);

        if (sec == null || sec < 0)
            return Duration.ZERO;

        return Duration.ofSeconds(sec);
    }

    @Override
    public void startCooldown(OtpPurpose purpose, String email, long cooldownSeconds) {
        redisTemplate.opsForValue().set(
                redisKeyHelper.cooldownKey(purpose, email),
                "1",
                cooldownSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void saveOtpSession(String sessionId, OtpSessionPayload payload, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(
                    redisKeyHelper.sessionKey(sessionId),
                    json,
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            throw new MethodArgumentNotValidException("Failed to serialize OTP session", e);
        }
    }

    @Override
    public Optional<OtpSessionPayload> getOtpSession(String sessionId) {
        String json = redisTemplate.opsForValue().get(
                redisKeyHelper.sessionKey(sessionId));

        if (json == null) return Optional.empty();

        try {
            return Optional.of(
                    objectMapper.readValue(json, OtpSessionPayload.class));

        } catch (JsonProcessingException e) {
            throw new MethodArgumentNotValidException("Failed to deserialize OTP session", e);
        }
    }

    @Override
    public void setActiveSessionPointer(OtpPurpose purpose, String email, String sessionId, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                redisKeyHelper.activeSessionKey(purpose, email),
                sessionId,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public Optional<String> getActiveSessionPointer(OtpPurpose purpose, String email) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(
                        redisKeyHelper.activeSessionKey(purpose, email)));
    }

    @Override
    public void deleteSession(String sessionId) {
        redisTemplate.delete(
                redisKeyHelper.sessionKey(sessionId));
    }
}
