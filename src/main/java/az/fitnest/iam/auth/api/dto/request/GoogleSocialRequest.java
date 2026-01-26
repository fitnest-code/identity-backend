package az.fitnest.iam.auth.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "idToken")
public class GoogleSocialRequest {

    @NotBlank
    @JsonAlias("id_token")
    private String idToken;

    @JsonAlias("full_name")
    private String fullName;

    @JsonAlias("email")
    private String email;
}