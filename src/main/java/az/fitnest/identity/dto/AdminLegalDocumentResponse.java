package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AdminLegalDocumentResponse(
    Long id,
    String type,
    String version,
    String language,
    String content,

    @JsonProperty("is_active")
    boolean isActive,

    @JsonProperty("published_at")
    LocalDateTime publishedAt,

    @JsonProperty("created_date")
    LocalDateTime createdDate,

    @JsonProperty("last_modified_date")
    LocalDateTime lastModifiedDate
) {}
