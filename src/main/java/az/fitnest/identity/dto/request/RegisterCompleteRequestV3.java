package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to complete registration V3 (passwordless) after OTP verification")
public record  RegisterCompleteRequestV3(
        @NotBlank(message = "Qeydiyyat tokeni tələb olunur")
        @JsonProperty("registration_token")
        @Schema(description = "Registration token received from OTP verification step", example = "uuid-token", requiredMode = Schema.RequiredMode.REQUIRED)
        String registrationToken,

        @NotBlank(message = "Ad tələb olunur")
        @Size(min = 2, max = 50, message = "Ad 2-50 simvol aralığında olmalıdır")
        @JsonProperty("first_name")
        @Schema(description = "User's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,

        @NotBlank(message = "Soyad tələb olunur")
        @Size(min = 2, max = 50, message = "Soyad 2-50 simvol aralığında olmalıdır")
        @JsonProperty("last_name")
        @Schema(description = "User's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,

        @JsonProperty("device_id")
        @Schema(description = "Unique identifier of the mobile device for binding", example = "uuid-1234-5678")
        String deviceId,

        @NotBlank(message = "Cihaz tipi tələb olunur")
        @Pattern(regexp = "^(iOS|Android|Web)$", message = "Yanlış cihaz tipi. iOS, Android və ya Web olmalıdır.")
        @JsonProperty("device_type")
        @Schema(description = "Device type (iOS, Android, Web)", example = "iOS", requiredMode = Schema.RequiredMode.REQUIRED)
        String deviceType
) {
}
