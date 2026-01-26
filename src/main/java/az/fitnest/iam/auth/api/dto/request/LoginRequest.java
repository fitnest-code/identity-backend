package az.fitnest.iam.auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "password")
public class LoginRequest {

    @NotBlank
    @Pattern(regexp = "^\\+\\d{8,15}$")
    private String mobile;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;
}