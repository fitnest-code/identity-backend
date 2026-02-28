package az.fitnest.identity.dto;
import az.fitnest.identity.model.enums.UserStatus;

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

    @JsonAlias("first_name")
    private String firstName;

    @JsonAlias("last_name")
    private String lastName;

    @JsonAlias("email")
    private String email;
}