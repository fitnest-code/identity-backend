package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshResponse(
    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("refresh_token")
    String refreshToken
) {}
