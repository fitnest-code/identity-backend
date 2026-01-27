package az.fitnest.iam.setup.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalsResponse {

    @JsonProperty("items")
    private List<GoalItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalItem {
        @JsonProperty("code")
        private String code;

        @JsonProperty("title")
        private String title;

        @JsonProperty("subtitle")
        private String subtitle;
    }
}
