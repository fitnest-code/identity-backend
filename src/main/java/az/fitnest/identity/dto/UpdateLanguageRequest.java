package az.fitnest.identity.dto;

import az.fitnest.identity.constants.Language;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLanguageRequest {

    @NotNull(message = "Language is required")
    private Language language;
}
