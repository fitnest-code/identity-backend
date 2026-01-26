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

    @JsonAlias("email")
    private String email;
}