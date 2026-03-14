package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ErrorWrapperResponse(
        @JsonProperty("error") ErrorDetailResponse error,
        String code,
        String message,
        int status,
        String path,
        LocalDateTime timestamp,
        String field,
        String issue
) {
    public static ErrorWrapperResponse fromErrorResponse(ErrorResponse errorResponse, int status) {
        Map<String, Object> details = null;
        if (errorResponse.details() != null && errorResponse.details().containsKey("validationErrors")) {
            @SuppressWarnings("unchecked")
            Map<String, String> validationErrors = (Map<String, String>) errorResponse.details().get("validationErrors");
            if (validationErrors != null) {
                List<FieldIssueResponse> issues = validationErrors.entrySet().stream()
                        .map(entry -> new FieldIssueResponse(entry.getKey(), entry.getValue()))
                        .toList();
                details = Map.of("validationErrors", issues);
            }
        }

        return new ErrorWrapperResponse(
                new ErrorDetailResponse(
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

    public record ErrorDetailResponse(
            String code,
            String message,
            int status,
            String path,
            @JsonFormat(pattern = "dd/MM/yyyy")
            LocalDateTime timestamp,
            Map<String, Object> details
    ) {}

    public record FieldIssueResponse(
            String field,
            String issue
    ) {}
}
