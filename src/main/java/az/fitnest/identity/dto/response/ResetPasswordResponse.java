package az.fitnest.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Şifrə sıfırlanması cavabı")
public record ResetPasswordResponse(
    @Schema(description = "Məlumat mesajı", example = "Şifrə uğurla dəyişdirildi")
    String message
) {}
