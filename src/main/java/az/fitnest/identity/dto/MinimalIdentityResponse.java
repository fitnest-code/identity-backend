package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MinimalIdentityResponse(
    @JsonProperty("user_id") Long userId,
    @JsonProperty("mobile") String mobile,
    @JsonProperty("email") String email
) {}
