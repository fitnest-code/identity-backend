package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank
        @JsonAlias({"refreshToken", "refresh_token"})
        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonAlias({"deviceType", "device_type"})
        @JsonProperty("device_type")
        String deviceType
) {
}

