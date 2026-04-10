package az.fitnest.identity.dto.response;

import az.fitnest.identity.dto.response.ApiErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Generic API Response wrapper. Cavab növündən asılı olaraq 'success' və ya 'error' açarı altında qaytarılır.")
public record ApiResponse<T>(
    @Schema(description = "Uğurlu cavab məlumatı. Əgər ApiSuccessResponse növündədirsə, 'success' açarı altında qaytarılır.", nullable = true)
    T data,
    @Schema(description = "Xəta məlumatı. 'error' açarı altında qaytarılır.", nullable = true)
    ApiErrorResponse error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> error(ApiErrorResponse apiError) {
        return new ApiResponse<>(null, apiError);
    }

    @JsonValue
    public Object asJson() {
        if (error != null) {
            return Map.of("error", error);
        }
        if (data instanceof ApiSuccessResponse success) {
            return Map.of("success", success);
        }
        return data != null ? data : Map.of();
    }
}
