package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateSetupRequiredRequest(
        @JsonProperty("setup_required")
        Boolean setupRequired
) {
}
