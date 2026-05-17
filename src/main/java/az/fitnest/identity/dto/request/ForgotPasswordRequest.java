package az.fitnest.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Şifrəni unutmuşam sorğusu")
public record ForgotPasswordRequest(
        @NotBlank(message = "Mobil nömrə tələb olunur")
        @jakarta.validation.constraints.Pattern(regexp = "^(\\+994|0)(10|50|51|55|60|70|77|99)\\d{7}$", message = "Yanlış mobil nömrə formatı")
        @Schema(description = "İstifadəçinin mobil nömrəsi", example = "0501234567")
        String mobile
) {
}
