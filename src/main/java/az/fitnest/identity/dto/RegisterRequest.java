package az.fitnest.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to initiate registration by collecting user data and sending OTP")
public record RegisterRequest(
        @NotBlank(message = "Mobil nömrə tələb olunur")
        @jakarta.validation.constraints.Pattern(regexp = "^(0|\\+994)(50|51|10|55|99|70|77|60)\\d{7}$", message = "Yanlış mobil nömrə formatı. Azərbaycan formatında olmalıdır (məsələn, 0501234567 və ya +994501234567).")
        @Schema(description = "User's mobile number", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
        String mobile
) {
}
