package az.fitnest.iam.setup.adapter.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CompleteSetupResponse {
    @JsonProperty("setup_required")
    private Boolean setupRequired;

    @JsonProperty("next")
    private NextSteps next;

    @Data
    public static class NextSteps {
        @JsonProperty("workout_plan_ready")
        private Boolean workoutPlanReady;

        @JsonProperty("nutrition_plan_ready")
        private Boolean nutritionPlanReady;
    }
}
