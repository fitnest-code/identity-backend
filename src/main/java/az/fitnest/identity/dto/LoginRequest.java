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
    @Pattern(regexp = "^(050|051|010|055|099|070|077|060)\\d{7}$", message = "Invalid mobile number format. Must start with 050, 051, 010, 055, 099, 070, 077, or 060 and follow with 7 digits.")
    @Schema(description = "User's mobile number in Azerbaijan format", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @NotBlank
    @Size(min = 8, max = 64)
    @Schema(description = "User's password (8-64 characters)", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 64)
    private String password;

    @Schema(description = "Type of device (e.g., Android, iOS)", example = "Android")
    private String deviceType;
}