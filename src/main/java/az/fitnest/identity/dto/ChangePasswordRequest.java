package az.fitnest.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank
        String oldPassword,

        @NotBlank
        String newPassword,

        @NotBlank
        String confirmNewPassword
) {
}
