package az.fitnest.iam.auth.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "Request to initiate registration by collecting user data and sending OTP")
public class RegisterRequest {



    @NotBlank(message = "Mobile number is required")
    @jakarta.validation.constraints.Pattern(regexp = "^(050|051|010|055|099|070|077|060)\\d{7}$", message = "Invalid mobile number format. Must start with 050, 051, 010, 055, 099, 070, 077, or 060 and follow with 7 digits.")
    @Schema(description = "User's mobile number", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;
}
