package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String oldPassword;
    @NotBlank
    private String newPassword;
    @NotBlank
    private String confirmNewPassword;
}

