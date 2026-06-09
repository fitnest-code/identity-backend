package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for login eligibility check")
public record LoginEligibilityResponse(
        @JsonProperty("is_eligible_to_login")
        @Schema(description = "Whether the user is eligible to login", example = "true")
        boolean isEligibleToLogin
) {
}
