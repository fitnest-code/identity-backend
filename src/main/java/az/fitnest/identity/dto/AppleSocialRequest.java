package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record AppleSocialRequest(
    @NotBlank
    @JsonAlias("identity_token")
    String identityToken,

    @JsonAlias("full_name")
    String fullName,

    @JsonAlias("first_name")
    String firstName,

    @JsonAlias("last_name")
    String lastName,

    @JsonAlias("email")
    String email
) {}