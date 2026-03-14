package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record GoogleSocialRequest(
    @NotBlank
    @JsonAlias("id_token")
    String idToken,

    @JsonAlias("full_name")
    String fullName,

    @JsonAlias("first_name")
    String firstName,

    @JsonAlias("last_name")
    String lastName,

    @JsonAlias("email")
    String email
) {}
