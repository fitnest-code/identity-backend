package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.service.RedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import az.fitnest.identity.repository.UserRepository;

@Component
public class RateLimitAdminService {
    private final StringRedisTemplate redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;
    private final PhoneNormalizer phoneNormalizer;
    private final UserRepository userRepository;
    private final az.fitnest.identity.service.UserProfileGrpcClient userProfileGrpcClient;

    public RateLimitAdminService(StringRedisTemplate redisTemplate, RedisKeyBuilder redisKeyBuilder, PhoneNormalizer phoneNormalizer, UserRepository userRepository, az.fitnest.identity.service.UserProfileGrpcClient userProfileGrpcClient) {
        this.redisTemplate = redisTemplate;
        this.redisKeyBuilder = redisKeyBuilder;
        this.phoneNormalizer = phoneNormalizer;
        this.userRepository = userRepository;
        this.userProfileGrpcClient = userProfileGrpcClient;
    }

    public void resetRateLimit(OtpPurpose purpose, String phoneNumber) {
        String normalizedPhone = phoneNormalizer.normalizeAzerbaijanPhoneNumber(phoneNumber);
        if (normalizedPhone == null) return;
        RedisKeyBuilder.RedisKeys keys = redisKeyBuilder.rateLimitKeys(purpose, normalizedPhone);
        redisTemplate.delete(List.of(keys.windowKey(), keys.cooldownKey()));
    }

    public RateLimitStatus getRateLimitStatus(OtpPurpose purpose, String phoneNumber) {
        String normalizedPhone = phoneNormalizer.normalizeAzerbaijanPhoneNumber(phoneNumber);
        if (normalizedPhone == null) return new RateLimitStatus(0, 0, 0);

        RedisKeyBuilder.RedisKeys keys = redisKeyBuilder.rateLimitKeys(purpose, normalizedPhone);
        String windowVal = redisTemplate.opsForValue().get(keys.windowKey());
        Long windowTtl = redisTemplate.getExpire(keys.windowKey(), TimeUnit.SECONDS);
        Long cdTtl = redisTemplate.getExpire(keys.cooldownKey(), TimeUnit.SECONDS);

        return new RateLimitStatus(
                parseLongSafely(windowVal),
                cdTtl != null && cdTtl > 0 ? cdTtl : 0,
                windowTtl != null && windowTtl > 0 ? windowTtl : 0
        );
    }

    private long parseLongSafely(String value) {
        if (value == null) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public void resetAllRateLimitsForUser(Long userId) {
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return;
        var user = userOpt.get();
        for (OtpPurpose purpose : OtpPurpose.values()) {
            String identifier = null;
            if (purpose == OtpPurpose.EMAIL_CHANGE) {
                try {
                    var profile = userProfileGrpcClient.getUserProfileDetails(userId);
                    if (profile != null && !profile.getEmail().isEmpty()) {
                        identifier = profile.getEmail();
                    }
                } catch (Exception e) {
                }
            } else {
                identifier = user.getMobile();
            }

            if (identifier != null) {
                RedisKeyBuilder.RedisKeys keys = redisKeyBuilder.rateLimitKeys(purpose, identifier);
                redisTemplate.delete(List.of(keys.windowKey(), keys.cooldownKey()));
                String dailyKey = redisKeyBuilder.rateLimitDailyAttemptsKey(purpose, identifier);
                redisTemplate.delete(dailyKey);
            }
        }
    }

    public void resetAllRateLimitsForAllUsers() {
        var users = userRepository.findAll();
        for (var user : users) {
            for (OtpPurpose purpose : OtpPurpose.values()) {
                String identifier = null;
                if (purpose == OtpPurpose.EMAIL_CHANGE) {
                    try {
                        var profile = userProfileGrpcClient.getUserProfileDetails(user.getId());
                        if (profile != null && !profile.getEmail().isEmpty()) {
                            identifier = profile.getEmail();
                        }
                    } catch (Exception e) {
                    }
                } else {
                    identifier = user.getMobile();
                }

                if (identifier != null) {
                    RedisKeyBuilder.RedisKeys keys = redisKeyBuilder.rateLimitKeys(purpose, identifier);
                    redisTemplate.delete(List.of(keys.windowKey(), keys.cooldownKey()));
                    String dailyKey = redisKeyBuilder.rateLimitDailyAttemptsKey(purpose, identifier);
                    redisTemplate.delete(dailyKey);
                }
            }
        }
    }

    public record RateLimitStatus(long attempts, long cooldownTtlSec, long windowTtlSec) {
    }
}
