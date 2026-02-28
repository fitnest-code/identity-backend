package az.fitnest.identity.dto;
import az.fitnest.identity.model.enums.UserStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateLanguageRequest {

    @NotBlank(message = "Language code is required")
    private String code;
}
