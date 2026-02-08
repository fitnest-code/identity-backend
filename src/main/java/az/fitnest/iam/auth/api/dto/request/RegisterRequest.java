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
    @jakarta.validation.constraints.Pattern(regexp = "^\\+\\d{8,15}$", message = "Invalid mobile number format")
    @Schema(description = "User's mobile number", example = "+994501234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;
}
