package az.fitnest.iam.auth.adapter.service;

import az.fitnest.iam.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RegistrationTokenService {

    private final StringRedisTemplate redisTemplate;
    
    @Value("${auth.registration-token.prefix:auth:registration:}")
    private String tokenPrefix;
    
    @Value("${auth.registration-token.ttl-hours:24}")
    private long ttlHours;

    public String issueForEmail(String email) {
        String token = UUID.randomUUID().toString();
        String key = registrationKey(token);
        redisTemplate.opsForValue().set(key, email, ttlHours, TimeUnit.HOURS);
        return token;
    }

    public String requireEmail(String token) {
        String key = registrationKey(token);
        String email = redisTemplate.opsForValue().get(key);
        
        if (email == null) {
            throw new UnauthorizedException("Registration token invalid or expired");
        }
        
        return email;
    }

    public void consume(String token) {
        String key = registrationKey(token);
        redisTemplate.delete(key);
    }

    private String registrationKey(String token) {
        return tokenPrefix + token;
    }
}
