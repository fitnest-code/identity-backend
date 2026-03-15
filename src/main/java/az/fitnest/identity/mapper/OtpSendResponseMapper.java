package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.response.OtpSendResponse;
import org.springframework.stereotype.Component;

@Component
public class OtpSendResponseMapper {
    public OtpSendResponse toResponse(String sessionId, Integer ttl, Integer cooldown, String message) {
        return new OtpSendResponse(sessionId, ttl, cooldown, message);
    }
}
