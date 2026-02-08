package az.fitnest.iam.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorWrapper {

    @JsonProperty("error")
    private ErrorDetail error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
        private int status;
        private String path;
        private LocalDateTime timestamp;
        private Map<String, Object> details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldIssue {
        private String field;
        private String issue;
    }

    public static ErrorWrapper fromErrorResponse(ErrorResponse errorResponse, int status) {
        Map<String, Object> details = null;
        if (errorResponse.getDetails() != null && errorResponse.getDetails().containsKey("validationErrors")) {
            @SuppressWarnings("unchecked")
            Map<String, String> validationErrors = (Map<String, String>) errorResponse.getDetails().get("validationErrors");
            if (validationErrors != null) {
                List<FieldIssue> issues = validationErrors.entrySet().stream()
                        .map(entry -> FieldIssue.builder()
                                .field(entry.getKey())
                                .issue(entry.getValue())
                                .build())
                        .toList();
                details = Map.of("validationErrors", issues);
            }
        }

        return ErrorWrapper.builder()
                .error(ErrorDetail.builder()
                        .code(errorResponse.getCode())
                        .message(errorResponse.getMessage())
                        .status(status)
                        .path(errorResponse.getPath())
                        .timestamp(errorResponse.getTimestamp())
                        .details(details)
                        .build())
                .build();
    }
}
