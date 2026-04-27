package az.fitnest.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for changing email address")
public record ChangeEmailRequest(
        @NotBlank
        @Email
        @Schema(description = "New email address", example = "newuser@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String newEmail
) {
}
