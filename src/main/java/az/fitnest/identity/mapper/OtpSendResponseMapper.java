package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.OtpSendResponse;

public final class OtpSendResponseMapper {
    private OtpSendResponseMapper() {}
    public static OtpSendResponse toResponse(String sessionId, Integer ttl, Integer cooldown, String message) {
        return new OtpSendResponse(sessionId, ttl, cooldown, message);
    }
}
