package az.fitnest.iam.user.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateSetupRequiredRequest {

    @JsonProperty("setup_required")
    private Boolean setupRequired;
}
