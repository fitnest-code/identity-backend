package az.fitnest.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "User login credentials V2 with optional device binding")
public record LoginRequestV2(
        @NotBlank(message = "Mobil nömrə tələb olunur")
        @Pattern(regexp = "^(\\+994|0)(10|50|51|55|60|70|77|99)\\d{7}$", message = "Yanlış mobil nömrə formatı. Azərbaycan formatında olmalıdır (məsələn, 0501234567 və ya +994501234567).")
        @Schema(description = "User's mobile number in Azerbaijan format", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
        String mobile,

        @NotBlank(message = "Şifrə tələb olunur")
        @Size(min = 8, max = 64, message = "Şifrə 8-64 simvol aralığında olmalıdır")
        @Pattern(regexp = "^\\S+$", message = "Şifrədə boşluq simvolu ola bilməz")
        @Schema(description = "User's password (8-64 characters)", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 64)
        String password,

        @JsonProperty("device_id")
        @Schema(description = "Unique identifier of the mobile device for binding", example = "uuid-1234-5678")
        String deviceId,

        @NotBlank(message = "Cihaz tipi tələb olunur")
        @Pattern(regexp = "^(iOS|Android|Web)$", message = "Yanlış cihaz tipi. iOS, Android və ya Web olmalıdır.")
        @JsonProperty("device_type")
        @Schema(description = "Device type (iOS, Android, Web)", example = "iOS", requiredMode = Schema.RequiredMode.REQUIRED)
        String deviceType
) {
    @Override
    public String toString() {
        return "LoginRequestV2(mobile=" + mobile + ", password=[PROTECTED], deviceId=" + deviceId + ", deviceType=" + deviceType + ")";
    }
}
