package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.LegalDocumentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLegalDocumentRequest(
        @NotNull
        LegalDocumentType type,

        @NotBlank
        String version,

        @NotBlank
        String language,

        @NotBlank
        String content,

        @JsonProperty("is_active")
        @NotNull
        Boolean isActive
) {
}
