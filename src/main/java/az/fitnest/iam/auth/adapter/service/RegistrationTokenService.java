package az.fitnest.iam.auth.adapter.service;

import az.fitnest.iam.otp.domain.enums.OtpPurpose;
import az.fitnest.iam.shared.exception.UnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RegistrationTokenService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${auth.registration-token.prefix:auth:registration:}")
    private String tokenPrefix;
    
    @Value("${auth.registration-token.ttl-hours:24}")
    private long ttlHours;


    public String issueForIdentifier(String identifier) {
        String token = UUID.randomUUID().toString();
        String key = registrationKey(token);
        
        RegistrationTokenPayload payload = new RegistrationTokenPayload(identifier, OtpPurpose.REGISTRATION);
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(key, payloadJson, ttlHours, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize registration token payload", e);
        }
        
        return token;
    }

    public RegistrationTokenPayload requirePayload(String token) {
        String key = registrationKey(token);
        String payloadJson = redisTemplate.opsForValue().get(key);
        
        if (payloadJson == null) {
            throw new UnauthorizedException("Registration token invalid or expired");
        }
        
        try {
            RegistrationTokenPayload payload = objectMapper.readValue(payloadJson, RegistrationTokenPayload.class);
            if (payload.getPurpose() != OtpPurpose.REGISTRATION) {
                throw new UnauthorizedException("Invalid registration token purpose");
            }
            return payload;
        } catch (JsonProcessingException e) {
            throw new UnauthorizedException("Invalid registration token format");
        }
    }

    public String requireIdentifier(String token) {
        return requirePayload(token).getIdentifier();
    }

    public void consume(String token) {
        String key = registrationKey(token);
        redisTemplate.delete(key);
    }

    private String registrationKey(String token) {
        return tokenPrefix + token;
    }

    private static class RegistrationTokenPayload {
        private String identifier;
        private OtpPurpose purpose;

        public RegistrationTokenPayload() {}

        public RegistrationTokenPayload(String identifier, OtpPurpose purpose) {
            this.identifier = identifier;
            this.purpose = purpose;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public OtpPurpose getPurpose() {
            return purpose;
        }

        public void setPurpose(OtpPurpose purpose) {
            this.purpose = purpose;
        }
    }
}
