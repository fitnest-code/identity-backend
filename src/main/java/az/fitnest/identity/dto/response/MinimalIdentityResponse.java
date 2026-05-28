package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MinimalIdentityResponse(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("mobile") String mobile,
        @JsonProperty("email") String email,
        @JsonProperty("has_local_password") boolean hasLocalPassword,
        @JsonProperty("is_eligible_to_have_local_password") boolean isEligibleToHaveLocalPassword
) {
}
