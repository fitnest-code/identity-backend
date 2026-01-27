package az.fitnest.iam.auth.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "identityToken")
public class AppleSocialRequest {

    @NotBlank
    @JsonAlias("identity_token")
    private String identityToken;

    @JsonAlias("full_name")
    private String fullName;

    @JsonAlias("first_name")
    private String firstName;

    @JsonAlias("last_name")
    private String lastName;

    @JsonAlias("email")
    private String email;
}