package az.fitnest.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body for changing mobile number")
public record ChangeMobileRequest(
    @NotBlank
    @Pattern(regexp = "^(050|051|055|070|077|099|010|060)\\d{7}$", message = "Phone must be in format 0501234567")
    @Schema(description = "New mobile number", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
    String newMobile
) {}
