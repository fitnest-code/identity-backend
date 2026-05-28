package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PasswordEligibilityResponse(
        @JsonProperty("has_local_password") boolean hasLocalPassword,
        @JsonProperty("is_eligible_to_have_local_password") boolean isEligibleToHaveLocalPassword
) {
}
