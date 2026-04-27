package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.OffsetDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Uğurlu API cavabı formatı")
public class ApiSuccessResponse {
    @Schema(description = "Uğur kodu", example = "success.password.changed")
    String code;

    @Schema(description = "Məlumat mesajı", example = "Şifrə uğurla dəyişdirildi")
    String message;

    @Schema(description = "HTTP status kodu", example = "200")
    Integer status;

    @Schema(description = "Sorğu yolu", example = "/api/v1/auth/password-recovery/reset-password")
    String path;

    @Schema(description = "Zaman damğası")
    OffsetDateTime timestamp;

    @Schema(description = "Əlavə təfərrüatlar")
    Object details;

    public ApiSuccessResponse(String code, String message, Integer status, String path, OffsetDateTime timestamp, Object details) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.path = path;
        this.timestamp = timestamp;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Integer getStatus() {
        return status;
    }

    public String getPath() {
        return path;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public Object getDetails() {
        return details;
    }
}
