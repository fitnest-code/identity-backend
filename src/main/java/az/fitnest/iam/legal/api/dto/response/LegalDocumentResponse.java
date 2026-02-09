package az.fitnest.iam.legal.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LegalDocumentResponse {
    private DocumentData document;

    @Data
    @Builder
    public static class DocumentData {
        private String type;
        private String version;
        private String title;
        private String content;
        
        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
    }
}
