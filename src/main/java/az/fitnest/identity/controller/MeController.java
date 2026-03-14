package az.fitnest.identity.controller;

import az.fitnest.identity.util.UserContext;
import az.fitnest.identity.dto.ApiResponse;
import az.fitnest.identity.dto.SuccessResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.dto.MinimalIdentityResponse;
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

    @Operation(summary = "E-poçt dəyişmə sorğusu", description = "Yeni e-poçt ünvanına OTP kodu göndərir. Cavabda otp_session_id qaytarılır ki, sonra /confirm endpoint-ində istifadə olunsun.")
    @PostMapping("/api/v1/me/change-email/request")
    public ResponseEntity<ApiResponse<az.fitnest.identity.dto.OtpSendResponse>> requestEmailChange(
            @RequestParam String newEmail) {
        Long userId = UserContext.getRequiredUserId();
        az.fitnest.identity.dto.OtpSendResponse otpResponse = userService.requestEmailChange(userId, newEmail);
        return ResponseEntity.ok(ApiResponse.success(otpResponse));
    }

    @Operation(summary = "E-poçt dəyişməsini təsdiqləyin", description = "OTP sessiya ID-si və OTP kodu vasitəsilə yeni e-poçt ünvanını təsdiqləyir.")
    @PostMapping("/api/v1/me/change-email/confirm")
    public ResponseEntity<ApiResponse<UserResponse>> confirmEmailChange(
            @Valid @RequestBody az.fitnest.identity.dto.OtpVerifyRequest request) {
        Long userId = UserContext.getRequiredUserId();
        User updated = userService.confirmEmailChange(userId, request.otpSessionId(), request.otpCode());
        return ResponseEntity.ok(ApiResponse.success(UserResponseMapper.toResponse(updated)));
    }

    @Operation(summary = "Mobil nömrə dəyişmə sorğusu", description = "Yeni mobil nömrəyə OTP kodu göndərir. Cavabda otp_session_id qaytarılır.")
    @PostMapping("/api/v1/me/change-mobile/request")
    public ResponseEntity<ApiResponse<az.fitnest.identity.dto.OtpSendResponse>> requestMobileChange(
            @RequestParam String newMobile) {
        Long userId = UserContext.getRequiredUserId();
        az.fitnest.identity.dto.OtpSendResponse otpResponse = userService.requestMobileChange(userId, newMobile);
        return ResponseEntity.ok(ApiResponse.success(otpResponse));
    }

    @Operation(summary = "Mobil nömrə dəyişməsini təsdiqləyin", description = "OTP sessiya ID-si və OTP kodu vasitəsilə yeni mobil nömrəni təsdiqləyir.")
    @PostMapping("/api/v1/me/change-mobile/confirm")
    public ResponseEntity<ApiResponse<UserResponse>> confirmMobileChange(
            @Valid @RequestBody az.fitnest.identity.dto.OtpVerifyRequest request) {
        Long userId = UserContext.getRequiredUserId();
        User updated = userService.confirmMobileChange(userId, request.otpSessionId(), request.otpCode());
        return ResponseEntity.ok(ApiResponse.success(UserResponseMapper.toResponse(updated)));
    }

    @PostMapping("/api/v1/me/change-password")
    @Operation(summary = "Şifrəni dəyişdirin")
    public ResponseEntity<ApiResponse<SuccessResponse>> changePassword(
            @Valid @RequestBody az.fitnest.identity.dto.ChangePasswordRequest request,
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

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}
