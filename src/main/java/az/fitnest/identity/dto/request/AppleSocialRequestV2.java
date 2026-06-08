package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AppleSocialRequestV2(
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
        String email,

        @JsonAlias("device_id")
        String deviceId,

        @NotBlank(message = "Cihaz tipi tələb olunur")
        @Pattern(regexp = "^(iOS|Android|Web)$", message = "Yanlış cihaz tipi. iOS, Android və ya Web olmalıdır.")
        @JsonAlias("device_type")
        String deviceType
) {
}
