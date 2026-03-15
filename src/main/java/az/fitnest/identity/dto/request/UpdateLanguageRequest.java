package az.fitnest.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateLanguageRequest(
    @NotBlank(message = "Language code is required")
    String code
) {}
