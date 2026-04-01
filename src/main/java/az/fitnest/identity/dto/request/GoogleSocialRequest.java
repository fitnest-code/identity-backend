package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record GoogleSocialRequest(
    @NotBlank
    @JsonAlias("id_token")
    String idToken
) {}
