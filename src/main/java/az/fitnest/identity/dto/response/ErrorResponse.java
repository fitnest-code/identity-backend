package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String message,
        String code,
        String path,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
        LocalDateTime timestamp,
        Map<String, Object> details
) {
    public static ErrorResponse of(String message, String code) {
        return new ErrorResponse(message, code, null, LocalDateTime.now(), null);
    }

    public static ErrorResponse of(String message, String code, String path) {
        return new ErrorResponse(message, code, path, LocalDateTime.now(), null);
    }

    public static ErrorResponse of(String message, String code, String path, Map<String, Object> details) {
        return new ErrorResponse(message, code, path, LocalDateTime.now(), details);
    }
}
