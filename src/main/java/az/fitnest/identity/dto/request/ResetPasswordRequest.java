package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Şifrə sıfırlanması sorğusu")
public record ResetPasswordRequest(
        @NotBlank
        @JsonProperty("reset_token")
        @Schema(description = "OTP təsdiqindən sonra alınan sıfırlama tokeni", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String resetToken,

        @NotBlank
        @Size(min = 8, message = "Şifrə ən az 8 simvol olmalıdır")
        @Pattern(regexp = "^\\S+$", message = "Şifrədə boşluq simvolu ola bilməz")
        @Schema(description = "Yeni şifrə", example = "newStrongPassword123!")
        String newPassword
) {
}
