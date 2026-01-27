package az.fitnest.iam.setup.adapter.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GoalsResponse {
    @JsonProperty("items")
    private List<GoalItem> items;

    @Data
    public static class GoalItem {
        @JsonProperty("code")
        private String code;

        @JsonProperty("title")
        private String title;

        @JsonProperty("subtitle")
        private String subtitle;
    }
}
