package az.fitnest.iam.auth.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to complete registration after OTP verification")
public class RegisterCompleteRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User's email address used during registration", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{4}$", message = "OTP code must be 4 digits")
    @JsonProperty("otp_code")
    @Schema(description = "The 4-digit OTP code sent to the user's email", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String otpCode;
}