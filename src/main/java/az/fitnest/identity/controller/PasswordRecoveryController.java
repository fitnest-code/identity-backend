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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/auth/password-recovery")
@RequiredArgsConstructor
@Tag(name = "Şifrə Bərpası", description = "Şifrəni bərpa etmək üçün endpointlər.")
public class PasswordRecoveryController {
    private static final Logger logger = LoggerFactory.getLogger(PasswordRecoveryController.class);

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Şifrəni unutmuşam", description = "Şifrəni bərpa etmək üçün OTP göndərir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP uğurla göndərildi", content = @Content(schema = @Schema(implementation = OtpSendResponse.class))),
            @ApiResponse(responseCode = "400", description = "Yanlış sorğu və ya doğrulama uğursuz oldu")
    })
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<OtpSendResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("Received forgotPassword request: {}", request);
        try {
            OtpSendResponse response = passwordResetService.forgotPassword(request);
            logger.info("ForgotPassword success for mobile: {}", request != null ? request.mobile() : null);
            return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(response));
        } catch (Exception ex) {
            logger.error("Error in forgotPassword for request: {}", request, ex);
            throw ex;
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Şifrəni sıfırlayın", description = "OTP və yeni şifrə ilə şifrəni sıfırlayır.")
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<ResetPasswordResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(passwordResetService.resetPassword(request)));
    }
}
