package az.fitnest.identity.user.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateSetupRequiredRequest {

    @JsonProperty("setup_required")
    private Boolean setupRequired;
}
