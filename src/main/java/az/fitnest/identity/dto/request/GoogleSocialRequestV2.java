package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * V2 Google social login. Name fields are optional fallbacks when the ID token
 * lacks given_name/family_name (restricted Workspace accounts, missing scope).
 * Android/iOS currently send idToken only — they should also send SDK profile names.
 */
public record GoogleSocialRequestV2(
        @NotBlank
        @JsonAlias("id_token")
        String idToken,

        @JsonAlias({"full_name", "fullName"})
        String fullName,

        @JsonAlias({"first_name", "firstName", "given_name", "givenName"})
        String firstName,

        @JsonAlias({"last_name", "lastName", "family_name", "familyName"})
        String lastName,

        @JsonAlias("device_id")
        String deviceId,

        @NotBlank(message = "Cihaz tipi tələb olunur")
        @Pattern(regexp = "^(iOS|Android|Web)$", message = "Yanlış cihaz tipi. iOS, Android və ya Web olmalıdır.")
        @JsonAlias("device_type")
        String deviceType
) {
}
