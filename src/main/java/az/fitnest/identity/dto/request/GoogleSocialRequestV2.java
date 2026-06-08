package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GoogleSocialRequestV2(
        @NotBlank
        @JsonAlias("id_token")
        String idToken,

        @JsonAlias("device_id")
        String deviceId,

        @NotBlank(message = "Cihaz tipi tələb olunur")
        @Pattern(regexp = "^(iOS|Android|Web)$", message = "Yanlış cihaz tipi. iOS, Android və ya Web olmalıdır.")
        @JsonAlias("device_type")
        String deviceType
) {
}
