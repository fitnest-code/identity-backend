package az.fitnest.iam.user.api.dto.request;

import az.fitnest.iam.user.domain.enums.Language;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLanguageRequest {

    @NotNull(message = "Language is required")
    private Language language;
}
