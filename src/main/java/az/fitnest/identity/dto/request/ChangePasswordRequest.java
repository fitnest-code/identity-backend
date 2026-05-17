package az.fitnest.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank
        String oldPassword,

        @NotBlank
        @Pattern(regexp = "^\\S+$", message = "Şifrədə boşluq simvolu ola bilməz")
        String newPassword,

        @NotBlank
        @Pattern(regexp = "^\\S+$", message = "Şifrədə boşluq simvolu ola bilməz")
        String confirmNewPassword
) {
}
