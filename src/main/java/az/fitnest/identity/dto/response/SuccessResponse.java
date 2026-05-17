package az.fitnest.identity.dto.response;

import java.time.OffsetDateTime;

public record SuccessResponse(
        String message,
        int status,
        String path,
        OffsetDateTime timestamp
) {
    public static SuccessResponse of(String message, String path) {
        return new SuccessResponse(message, 200, path, OffsetDateTime.now());
    }
}
