package az.fitnest.identity.service;

import az.fitnest.identity.constants.OtpPurpose;
import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

public interface RegistrationTokenService {
    String issueForIdentifier(String identifier);
    RegistrationTokenPayload requirePayload(String token);
    String requireIdentifier(String token);
    void consume(String token);
    String getIdentifier();
    void setIdentifier(String identifier);
    OtpPurpose getPurpose();
    void setPurpose(OtpPurpose purpose);
}
