package az.fitnest.identity.dto;

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
    @jakarta.validation.constraints.Pattern(regexp = "^(0|\\+994)(50|51|10|55|99|70|77|60)\\d{7}$", message = "Invalid mobile number format. Must be in Azerbaijan format (e.g., 0501234567 or +994501234567).")
    @Schema(description = "User's mobile number", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;
}
