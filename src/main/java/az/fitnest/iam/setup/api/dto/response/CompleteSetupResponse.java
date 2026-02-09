package az.fitnest.iam.setup.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteSetupResponse {

    @JsonProperty("setup_required")
    private Boolean setupRequired;

    @JsonProperty("next")
    private NextSteps next;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextSteps {
        @JsonProperty("workout_plan_ready")
        private Boolean workoutPlanReady;

        @JsonProperty("nutrition_plan_ready")
        private Boolean nutritionPlanReady;
    }
}
