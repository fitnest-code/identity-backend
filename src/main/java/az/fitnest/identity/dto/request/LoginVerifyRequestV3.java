package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to verify login V3 with OTP code and device info")
public record LoginVerifyRequestV3(
        @NotBlank(message = "OTP sessiya ID-si tələb olunur")
        @JsonProperty("otp_session_id")
        @Schema(description = "OTP session ID received from login initiation", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        String otpSessionId,

        @NotBlank(message = "OTP kodu tələb olunur")
        @Pattern(regexp = "^\\d{4}$", message = "OTP kodu 4 rəqəm olmalıdır")
        @JsonProperty("otp_code")
        @Schema(description = "4-digit OTP code", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
        String otpCode,

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
