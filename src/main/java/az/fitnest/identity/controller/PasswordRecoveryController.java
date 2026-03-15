package az.fitnest.identity.controller;

import az.fitnest.identity.dto.request.*;
import az.fitnest.identity.dto.response.*;
import az.fitnest.identity.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-recovery")
@RequiredArgsConstructor
@Tag(name = "Şifrə Bərpası", description = "Şifrəni bərpa etmək üçün endpointlər.")
public class PasswordRecoveryController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Şifrəni unutmuşam", description = "Şifrəni bərpa etmək üçün OTP göndərir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP uğurla göndərildi", content = @Content(schema = @Schema(implementation = OtpSendResponse.class))),
            @ApiResponse(responseCode = "400", description = "Yanlış sorğu və ya doğrulama uğursuz oldu")
    })
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<OtpSendResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(passwordResetService.forgotPassword(request)));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Şifrəni sıfırlayın", description = "OTP və yeni şifrə ilə şifrəni sıfırlayır.")
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<ResetPasswordResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(passwordResetService.resetPassword(request)));
    }
}
