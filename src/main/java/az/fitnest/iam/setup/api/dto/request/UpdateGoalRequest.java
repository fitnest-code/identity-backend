package az.fitnest.iam.setup.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateGoalRequest {

    @NotBlank(message = "goal is required")
    @JsonProperty("goal")
    private String goal;
}
