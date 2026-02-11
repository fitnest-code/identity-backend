package az.fitnest.identity.service.impl;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;

import az.fitnest.identity.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ResetPasswordTokenServiceImpl implements ResetPasswordTokenService {

    private final StringRedisTemplate redisTemplate;
    
    @Value("${auth.reset-password-token.prefix:auth:reset-password:}")
    private String tokenPrefix;
    
    @Value("${auth.reset-password-token.ttl-hours:1}")
    private long ttlHours;

        @Override
    public String issueForIdentifier(String identifier) {
        String token = UUID.randomUUID().toString();
        String key = resetPasswordKey(token);
        redisTemplate.opsForValue().set(key, identifier, ttlHours, TimeUnit.HOURS);
        return token;
    }

        @Override
    public String requireIdentifier(String token) {
        String key = resetPasswordKey(token);
        String identifier = redisTemplate.opsForValue().get(key);
        
        if (identifier == null) {
            throw new UnauthorizedException("Reset password token invalid or expired");
        }
        
        return identifier;
    }

        @Override
    public void consume(String token) {
        String key = resetPasswordKey(token);
        redisTemplate.delete(key);
    }

    private String resetPasswordKey(String token) {
        return tokenPrefix + token;
    }
}
