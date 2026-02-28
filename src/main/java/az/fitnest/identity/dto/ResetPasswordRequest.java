package az.fitnest.identity.dto;
import az.fitnest.identity.model.enums.UserStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    
    @NotBlank(message = "Sıfırlama tokeni tələb olunur")
    private String resetToken;
    
    @NotBlank(message = "Yeni şifrə tələb olunur")
    @Size(min = 8, message = "Şifrə ən azı 8 simvoldan ibarət olmalıdır")
    private String newPassword;

    @NotBlank(message = "Şifrə təsdiqi tələb olunur")
    private String confirmPassword;
}
