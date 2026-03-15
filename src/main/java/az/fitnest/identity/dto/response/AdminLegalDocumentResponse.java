package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "dd/MM/yyyy")
    LocalDateTime publishedAt,

    @JsonProperty("created_date")
    @JsonFormat(pattern = "dd/MM/yyyy")
    LocalDateTime createdDate,

    @JsonProperty("last_modified_date")
    @JsonFormat(pattern = "dd/MM/yyyy")
    LocalDateTime lastModifiedDate
) {}
