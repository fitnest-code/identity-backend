package az.fitnest.identity.dto;
import az.fitnest.identity.model.enums.UserStatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to complete registration after OTP verification")
public class RegisterCompleteRequest {

    @NotBlank(message = "Qeydiyyat tokeni tələb olunur")
    @JsonProperty("registration_token")
    @Schema(description = "Registration token received from OTP verification step", example = "uuid-token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registrationToken;

    @NotBlank(message = "Ad tələb olunur")
    @Size(min = 2, max = 50, message = "Ad 2-50 simvol aralığında olmalıdır")
    @JsonProperty("first_name")
    @Schema(description = "User's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Soyad tələb olunur")
    @Size(min = 2, max = 50, message = "Soyad 2-50 simvol aralığında olmalıdır")
    @JsonProperty("last_name")
    @Schema(description = "User's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotBlank(message = "Şifrə tələb olunur")
    @Size(min = 8, max = 100, message = "Şifrə 8-100 simvol aralığında olmalıdır")
    @Schema(description = "User's password (min 8 characters)", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}