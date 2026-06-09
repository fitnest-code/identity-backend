package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to check login eligibility V3 (pre-login validation)")
public record LoginCheckRequestV3(
        @NotBlank(message = "Mobil nömrə tələb olunur")
        @Pattern(regexp = "^(\\+994|0)(10|50|51|55|60|70|77|99)\\d{7}$", message = "Yanlış mobil nömrə formatı. Azərbaycan formatında olmalıdır (məsələn, 0501234567 və ya +994501234567).")
        @Schema(description = "User's mobile number", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
        String mobile,

        @JsonProperty("device_id")
        @Schema(description = "Unique identifier of the mobile device", example = "uuid-1234-5678")
        String deviceId,

        @NotBlank(message = "Cihaz tipi tələb olunur")
        @Pattern(regexp = "^(iOS|Android|Web)$", message = "Yanlış cihaz tipi. iOS, Android və ya Web olmalıdır.")
        @JsonProperty("device_type")
        @Schema(description = "Device type (iOS, Android, Web)", example = "iOS", requiredMode = Schema.RequiredMode.REQUIRED)
        String deviceType
) {
}
