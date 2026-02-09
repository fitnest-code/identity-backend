package az.fitnest.iam.setup.adapter.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SetupStatusResponse {
    @JsonProperty("setup_required")
    private Boolean setupRequired;

    @JsonProperty("profile")
    private ProfileData profile;

    @JsonProperty("goal")
    private String goal;

    @Data
    public static class ProfileData {
        @JsonProperty("height_cm")
        private Integer heightCm;

        @JsonProperty("weight_kg")
        private Double weightKg;

        @JsonProperty("gender")
        private String gender;

        @JsonProperty("age")
        private Integer age;
    }
}
