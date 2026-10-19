package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * V1 Google social login. Optional name fields mirror Apple — clients should send
 * given/family/display from the Google SDK when the ID token omits profile claims.
 */
public record GoogleSocialRequest(
        @NotBlank
        @JsonAlias("id_token")
        String idToken,

        @JsonAlias({"full_name", "fullName"})
        String fullName,

        @JsonAlias({"first_name", "firstName", "given_name", "givenName"})
        String firstName,

        @JsonAlias({"last_name", "lastName", "family_name", "familyName"})
        String lastName
) {
}
