package az.fitnest.identity.user.api.dto.request;

import az.fitnest.identity.user.domain.enums.Language;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLanguageRequest {

    @NotNull(message = "Language is required")
    private Language language;
}
