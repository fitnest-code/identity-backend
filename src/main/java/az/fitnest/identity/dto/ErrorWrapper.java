package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ErrorWrapper(
        @JsonProperty("error") ErrorDetail error,
        String code,
        String message,
        int status,
        String path,
        LocalDateTime timestamp,
        String field,
        String issue
) {
    public static ErrorWrapper fromErrorResponse(ErrorResponse errorResponse, int status) {
        Map<String, Object> details = null;
        if (errorResponse.details() != null && errorResponse.details().containsKey("validationErrors")) {
            @SuppressWarnings("unchecked")
            Map<String, String> validationErrors = (Map<String, String>) errorResponse.details().get("validationErrors");
            if (validationErrors != null) {
                List<FieldIssue> issues = validationErrors.entrySet().stream()
                        .map(entry -> new FieldIssue(entry.getKey(), entry.getValue()))
                        .toList();
                details = Map.of("validationErrors", issues);
            }
        }

        return new ErrorWrapper(
                new ErrorDetail(
                        errorResponse.code(),
                        errorResponse.message(),
                        status,
                        errorResponse.path(),
                        errorResponse.timestamp(),
                        details
                ),
                null, null, status, null, null, null, null
        );
    }

    public record ErrorDetail(
            String code,
            String message,
            int status,
            String path,
            @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
            LocalDateTime timestamp,
            Map<String, Object> details
    ) {}

    public record FieldIssue(
            String field,
            String issue
    ) {}
}
