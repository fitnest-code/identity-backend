package az.fitnest.identity.controller;

import az.fitnest.identity.util.UserContext;
import az.fitnest.identity.dto.response.ApiResponse;
import az.fitnest.identity.dto.response.SuccessResponse;
import az.fitnest.identity.dto.response.UserResponse;
import az.fitnest.identity.dto.response.MinimalIdentityResponse;
import az.fitnest.identity.dto.request.OtpSendRequest;
import az.fitnest.identity.dto.request.OtpVerifyRequest;
import az.fitnest.identity.dto.request.ChangePasswordRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import az.fitnest.identity.dto.response.ApiErrorResponse;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mən", description = "Cari autentifikasiya olunmuş istifadəçi üçün hesab və profil idarəetməsi endpointləri.")
@SecurityRequirement(name = "bearerAuth")
public class MeController {

    private final UserService userService;
    private final MessageSource messageSource;
    private final UserResponseMapper userResponseMapper;

    @Operation(summary = "Cari istifadəçini əldə edin", description = "Autentifikasiya olunmuş istifadəçinin hesab təfərrüatlarını qaytarır.")
    @GetMapping("/api/v1/identity/me")
    public ResponseEntity<ApiResponse<MinimalIdentityResponse>> getMe() {
        Long userId = UserContext.getRequiredUserId();
        User user = userService.getUserById(userId);
        MinimalIdentityResponse response = new MinimalIdentityResponse(
            user.getId(),
            user.getMobile(),
            user.getEmail()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "E-poçt dəyişmə sorğusu",
        description = "Yeni e-poçt ünvanına OTP kodu göndərir. Cavabda otp_session_id qaytarılır ki, sonra /confirm endpoint-ində istifadə olunsun.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Yeni e-poçt ünvanı üçün sorğu",
            content = @io.swagger.v3.oas.annotations.media.Content(
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = az.fitnest.identity.dto.request.ChangeEmailRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "OTP uğurla göndərildi",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = az.fitnest.identity.dto.response.OtpSendResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Yanlış və ya natamam sorğu",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = az.fitnest.identity.dto.response.ApiErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "E-poçt artıq istifadə olunur və ya dəyişdirilə bilməz",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = az.fitnest.identity.dto.response.ApiErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Daxili server xətası",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = az.fitnest.identity.dto.response.ApiErrorResponse.class)
                )
            )
        }
    )
    @PostMapping("/api/v1/me/change-email/request")
    public ResponseEntity<Object> requestEmailChange(
            @Valid @RequestBody az.fitnest.identity.dto.request.ChangeEmailRequest request,
            HttpServletRequest servletRequest) {
        Long userId = UserContext.getRequiredUserId();
        az.fitnest.identity.dto.response.OtpSendResponse otpResponse = userService.requestEmailChange(userId, request.newEmail());
        // Build a success response in the same structure as error
        var now = java.time.OffsetDateTime.now();
        var success = az.fitnest.identity.dto.response.ApiSuccessResponse.builder()
                .code("success.email.change_requested")
                .message("Change email request sent successfully")
                .status(200)
                .path(servletRequest.getRequestURI())
                .timestamp(now)
                .details(otpResponse)
                .build();
        return ResponseEntity.ok(Map.of("success", success));
    }

    @Operation(summary = "E-poçt dəyişməsini təsdiqləyin", description = "OTP sessiya ID-si və OTP kodu vasitəsilə yeni e-poçt ünvanını təsdiqləyir.")
    @PostMapping("/api/v1/me/change-email/confirm")
    public ResponseEntity<ApiResponse<UserResponse>> confirmEmailChange(
            @Valid @RequestBody az.fitnest.identity.dto.request.OtpVerifyRequest request) {
        Long userId = UserContext.getRequiredUserId();
        User updated = userService.confirmEmailChange(userId, request.otpSessionId(), request.otpCode());
        return ResponseEntity.ok(ApiResponse.success(userResponseMapper.toResponse(updated)));
    }

    @Operation(summary = "Mobil nömrə dəyişmə sorğusu", description = "Yeni mobil nömrəyə OTP kodu göndərir. Cavabda otp_session_id qaytarılır.")
    @PostMapping("/api/v1/me/change-mobile/request")
    public ResponseEntity<ApiResponse<az.fitnest.identity.dto.response.OtpSendResponse>> requestMobileChange(
            @RequestParam String newMobile) {
        Long userId = UserContext.getRequiredUserId();
        az.fitnest.identity.dto.response.OtpSendResponse otpResponse = userService.requestMobileChange(userId, newMobile);
        return ResponseEntity.ok(ApiResponse.success(otpResponse));
    }

    @Operation(summary = "Mobil nömrə dəyişməsini təsdiqləyin", description = "OTP sessiya ID-si və OTP kodu vasitəsilə yeni mobil nömrəni təsdiqləyir.")
    @PostMapping("/api/v1/me/change-mobile/confirm")
    public ResponseEntity<ApiResponse<UserResponse>> confirmMobileChange(
            @Valid @RequestBody az.fitnest.identity.dto.request.OtpVerifyRequest request) {
        Long userId = UserContext.getRequiredUserId();
        User updated = userService.confirmMobileChange(userId, request.otpSessionId(), request.otpCode());
        return ResponseEntity.ok(ApiResponse.success(userResponseMapper.toResponse(updated)));
    }

    @PostMapping("/api/v1/me/change-password")
    @Operation(summary = "Şifrəni dəyişdirin")
    public ResponseEntity<ApiResponse<SuccessResponse>> changePassword(
            @Valid @RequestBody az.fitnest.identity.dto.request.ChangePasswordRequest request,
            HttpServletRequest servletRequest) {
        Long userId = UserContext.getRequiredUserId();
        userService.changePassword(userId, request.oldPassword(), request.newPassword(), request.confirmNewPassword());
        return ResponseEntity.ok(ApiResponse.success(
                SuccessResponse.of(getMessage("success.password.changed"), servletRequest.getRequestURI())
        ));
    }

    @PostMapping("/api/v1/me/delete-account")
    @Operation(summary = "Delete account", description = "Marks the authenticated user's account as deleted. User can reactivate within 30 days.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<ApiResponse<SuccessResponse>> deleteAccount(HttpServletRequest request) {
        Long userId = UserContext.getRequiredUserId();
        try {
            userService.deleteAccount(userId);
            return ResponseEntity.ok(ApiResponse.success(
                    SuccessResponse.of(getMessage("success.account.deleted"), request.getRequestURI())
            ));
        } catch (Exception e) {
            String errorMsg = "Unexpected error during account deletion: " + e.getMessage();
            return ResponseEntity.status(500).body(ApiResponse.error(
                    ApiErrorResponse.builder()
                        .code("INTERNAL_SERVER_ERROR")
                        .message(errorMsg)
                        .status(500)
                        .path(request.getRequestURI())
                        .build()
            ));
        }
    }

    @Operation(summary = "E-poçt OTP-ni yenidən göndərin", description = "Cari istifadəçi üçün e-poçt dəyişmə OTP kodunu yenidən göndərir.")
    @PostMapping("/api/v1/me/change-email/resend")
    public ResponseEntity<ApiResponse<az.fitnest.identity.dto.response.OtpSendResponse>> resendEmailChangeOtp(
            @RequestParam String otpSessionId) {
        Long userId = UserContext.getRequiredUserId();
        az.fitnest.identity.dto.response.OtpSendResponse otpResponse = userService.resendEmailChangeOtp(userId, otpSessionId);
        return ResponseEntity.ok(ApiResponse.success(otpResponse));
    }

    @Operation(summary = "Mobil OTP-ni yenidən göndərin", description = "Cari istifadəçi üçün mobil dəyişmə OTP kodunu yenidən göndərir.")
    @PostMapping("/api/v1/me/change-mobile/resend")
    public ResponseEntity<ApiResponse<az.fitnest.identity.dto.response.OtpSendResponse>> resendMobileChangeOtp(
            @RequestParam String otpSessionId) {
        Long userId = UserContext.getRequiredUserId();
        az.fitnest.identity.dto.response.OtpSendResponse otpResponse = userService.resendMobileChangeOtp(userId, otpSessionId);
        return ResponseEntity.ok(ApiResponse.success(otpResponse));
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}
