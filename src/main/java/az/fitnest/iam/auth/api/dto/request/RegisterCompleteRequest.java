package az.fitnest.iam.auth.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "password")
public class RegisterCompleteRequest {

    @NotBlank
    @JsonAlias("full_name")
    private String fullName;

    @NotBlank
    @Size(min = 8, max = 10)
    private String password;
}