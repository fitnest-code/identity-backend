package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record LegalDocumentResponse(
    String version,
    String title,
    String content,

    @JsonProperty("updated_at")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
    LocalDateTime updatedAt
) {}
