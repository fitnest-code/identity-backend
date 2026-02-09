package az.fitnest.iam.legal.api.dto.request;

import az.fitnest.iam.legal.domain.enums.LegalDocumentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLegalDocumentRequest {

    @NotNull
    private LegalDocumentType type;

    @NotBlank
    private String version;

    @NotBlank
    private String language;

    @NotBlank
    private String content;

    @NotNull
    @JsonProperty("is_active")
    private Boolean isActive;
}
