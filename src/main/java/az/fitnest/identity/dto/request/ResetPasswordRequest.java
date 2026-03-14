package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank
    @JsonProperty("reset_token")
    String resetToken,

    @NotBlank
    @Size(min = 8, message = "Şifrə ən az 8 simvol olmalıdır")
    String newPassword
) {}
