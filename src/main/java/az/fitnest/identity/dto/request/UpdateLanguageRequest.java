package az.fitnest.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateLanguageRequest(
    @NotBlank(message = "Language code is required")
    String code
) {}
