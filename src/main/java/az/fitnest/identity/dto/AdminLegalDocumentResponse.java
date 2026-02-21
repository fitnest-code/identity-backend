package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminLegalDocumentResponse {

    private Long id;

    private String type;

    private String version;

    private String language;

    private String content;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("published_at")
    private LocalDateTime publishedAt;

    @JsonProperty("created_date")
    private LocalDateTime createdDate;

    @JsonProperty("last_modified_date")
    private LocalDateTime lastModifiedDate;
}
