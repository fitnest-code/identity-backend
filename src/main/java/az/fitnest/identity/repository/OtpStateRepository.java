package az.fitnest.identity.repository;

import az.fitnest.identity.model.otp.OtpUserState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class OtpStateRepository {
    private static final String OTP_STATE_KEY_PREFIX = "otp:global:";
    private static final Duration STATE_TTL = Duration.ofDays(1);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Optional<OtpUserState> get(String key) {
        Object obj = redisTemplate.opsForValue().get(OTP_STATE_KEY_PREFIX + key);
        if (obj instanceof OtpUserState state) {
            return Optional.of(state);
        }
        return Optional.empty();
    }

    public void save(OtpUserState state) {
        redisTemplate.opsForValue().set(OTP_STATE_KEY_PREFIX + state.getKey(), state, STATE_TTL);
    }

    public void delete(String key) {
        redisTemplate.delete(OTP_STATE_KEY_PREFIX + key);
    }
}
