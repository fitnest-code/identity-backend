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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.web.bind.annotation.RequestParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

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
            @ApiResponse(responseCode = "200", description = "OTP uğurla göndərildi",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiSuccessResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "SuccessResponse",
                                    value = "{\"success\": {\"code\": \"success.otp.sent\", \"message\": \"Təsdiq kodu göndərildi\", \"status\": 200, \"path\": \"/api/v1/auth/password-recovery/forgot-password\", \"timestamp\": \"2026-04-10T11:39:29.183Z\", \"details\": {\"otp_session_id\": \"550e8400-e29b-41d4-a716-446655440000\", \"expires_in_seconds\": 180, \"resend_available_in_seconds\": 60, \"message\": \"Təsdiq kodu göndərildi\"}}}"
                            ))),
            @ApiResponse(responseCode = "400", description = "Yanlış sorğu və ya doğrulama uğursuz oldu",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "ErrorResponse",
                                    value = "{\"error\": {\"code\": \"error.validation\", \"message\": \"Verilən məlumatlar etibarsızdır\", \"status\": 400, \"path\": \"/api/v1/auth/password-recovery/forgot-password\", \"timestamp\": \"2026-04-10T11:39:29.183Z\"}}"
                            )))
    })
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<ApiSuccessResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        logger.info("Received forgotPassword request: {}", request);
        try {
            OtpSendResponse response = passwordResetService.forgotPassword(request);
            logger.info("ForgotPassword success for mobile: {}", request != null ? request.mobile() : null);

            ApiSuccessResponse apiSuccess = ApiSuccessResponse.builder()
                    .code("success.otp.sent")
                    .message(response.message())
                    .status(HttpStatus.OK.value())
                    .path(httpRequest.getRequestURI())
                    .timestamp(OffsetDateTime.now())
                    .details(response)
                    .build();

            return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(apiSuccess));
        } catch (Exception ex) {
            logger.error("Error in forgotPassword for request: {}", request, ex);
            throw ex;
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Şifrəni sıfırlayın", description = "OTP və yeni şifrə ilə şifrəni sıfırlayır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Şifrə uğurla sıfırlandı",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiSuccessResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "SuccessResponse",
                                    value = "{\"success\": {\"code\": \"success.password.changed\", \"message\": \"Şifrə uğurla dəyişdirildi\", \"status\": 200, \"path\": \"/api/v1/auth/password-recovery/reset-password\", \"timestamp\": \"2026-04-10T11:39:29.183Z\"}}"
                            ))),
            @ApiResponse(responseCode = "400", description = "Yanlış sorğu və ya doğrulama uğursuz oldu",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "ErrorResponse",
                                    value = "{\"error\": {\"code\": \"error.validation\", \"message\": \"Verilən məlumatlar etibarsızdır\", \"status\": 400, \"path\": \"/api/v1/auth/password-recovery/reset-password\", \"timestamp\": \"2026-04-10T11:39:29.183Z\"}}"
                            )))
    })
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<ApiSuccessResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        ResetPasswordResponse response = passwordResetService.resetPassword(request);

        ApiSuccessResponse apiSuccess = ApiSuccessResponse.builder()
                .code("success.password.changed")
                .message(response.message())
                .status(HttpStatus.OK.value())
                .path(httpRequest.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(apiSuccess));
    }

    @PostMapping("/admin/forgot-password")
    @Operation(summary = "Admin şifrəni unutmuşam", description = "Admin şifrə bərpası üçün OTP göndərir. Yalnız admin rollarına icazə verilir.")
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<ApiSuccessResponse>> adminForgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        logger.info("Received admin forgotPassword request: {}", request);
        try {
            OtpSendResponse response = passwordResetService.adminForgotPassword(request);
            logger.info("Admin ForgotPassword success for mobile: {}", request != null ? request.mobile() : null);

            ApiSuccessResponse apiSuccess = ApiSuccessResponse.builder()
                    .code("success.otp.sent")
                    .message(response.message())
                    .status(HttpStatus.OK.value())
                    .path(httpRequest.getRequestURI())
                    .timestamp(OffsetDateTime.now())
                    .details(response)
                    .build();

            return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(apiSuccess));
        } catch (Exception ex) {
            logger.error("Error in admin forgotPassword for request: {}", request, ex);
            throw ex;
        }
    }

    @PostMapping("/admin/reset-password")
    @Operation(summary = "Admin şifrəni sıfırlayın", description = "OTP və yeni şifrə ilə admin şifrəsini sıfırlayır.")
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<ApiSuccessResponse>> adminResetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        ResetPasswordResponse response = passwordResetService.adminResetPassword(request);

        ApiSuccessResponse apiSuccess = ApiSuccessResponse.builder()
                .code("success.password.changed")
                .message(response.message())
                .status(HttpStatus.OK.value())
                .path(httpRequest.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(apiSuccess));
    }
}
