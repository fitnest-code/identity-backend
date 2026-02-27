package az.fitnest.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Request DTO for user login.
 * Contains credentials required for authentication.
 */
@Getter
@Setter
@ToString(exclude = "password")
@Schema(description = "User login credentials")
public class LoginRequest {

    @NotBlank
    @Pattern(regexp = "^(0|\\+994)(50|51|10|55|99|70|77|60)\\d{7}$", message = "Invalid mobile number format. Must be in Azerbaijan format (e.g., 0501234567 or +994501234567).")
    @Schema(description = "User's mobile number in Azerbaijan format", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @NotBlank
    @Size(min = 8, max = 64)
    @Schema(description = "User's password (8-64 characters)", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 64)
    private String password;
}