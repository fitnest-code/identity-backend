package az.fitnest.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "User login credentials")
public record LoginRequest(
        @NotBlank(message = "Mobil nömrə tələb olunur")
        @Pattern(regexp = "^(\\+994|0)(10|50|51|55|60|70|77|99)\\d{7}$", message = "Yanlış mobil nömrə formatı. Azərbaycan formatında olmalıdır (məsələn, 0501234567 və ya +994501234567).")
        @Schema(description = "User's mobile number in Azerbaijan format", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
        String mobile,

        @NotBlank(message = "Şifrə tələb olunur")
        @Size(min = 8, max = 64, message = "Şifrə 8-64 simvol aralığında olmalıdır")
        @Pattern(regexp = "^\\S+$", message = "Şifrədə boşluq simvolu ola bilməz")
        @Schema(description = "User's password (8-64 characters)", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 64)
        String password
) {
    @Override
    public String toString() {
        return "LoginRequest(mobile=" + mobile + ", password=[PROTECTED])";
    }
}
