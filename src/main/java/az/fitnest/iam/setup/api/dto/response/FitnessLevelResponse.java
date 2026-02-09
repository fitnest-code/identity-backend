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
public class FitnessLevelResponse {

    @JsonProperty("bmi")
    private Double bmi;

    @JsonProperty("bmi_category")
    private String bmiCategory;

    @JsonProperty("bmi_scale")
    private BmiScale bmiScale;

    @JsonProperty("goal")
    private String goal;

    @JsonProperty("message")
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BmiScale {
        @JsonProperty("underweight_max")
        private Double underweightMax;

        @JsonProperty("normal_max")
        private Double normalMax;

        @JsonProperty("overweight_max")
        private Double overweightMax;
    }
}
