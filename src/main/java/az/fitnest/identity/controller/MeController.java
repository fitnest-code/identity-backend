package az.fitnest.identity.controller;

import az.fitnest.identity.criteria.UserContext;
import az.fitnest.identity.dto.ApiResponse;
import az.fitnest.identity.dto.UserResponse;
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

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Me", description = "Cari istifadəçi məlumatlarının idarə olunması üçün ucluqlar")
@SecurityRequirement(name = "bearerAuth")
public class MeController {

    private final UserService userService;

    @Operation(summary = "Cari istifadəçini əldə edin", description = "Autentifikasiya olunmuş istifadəçinin hesab təfərrüatlarını qaytarır.")
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        Long userId = UserContext.getRequiredUserId();
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(UserResponseMapper.toResponse(user)));
    }

    @Operation(summary = "E-poçt dəyişmə sorğusu", description = "Yeni e-poçt ünvanına OTP kodu göndərir. Cavabda otp_session_id qaytarılır ki, sonra /confirm endpoint-ində istifadə olunsun.")
    @PostMapping("/change-email/request")
    public ResponseEntity<ApiResponse<az.fitnest.identity.dto.OtpSendResponse>> requestEmailChange(
            @RequestParam String newEmail) {
        Long userId = UserContext.getRequiredUserId();
        az.fitnest.identity.dto.OtpSendResponse otpResponse = userService.requestEmailChange(userId, newEmail);
        return ResponseEntity.ok(ApiResponse.success(otpResponse));
    }

    @Operation(summary = "E-poçt dəyişməsini təsdiqləyin", description = "OTP sessiya ID-si və OTP kodu vasitəsilə yeni e-poçt ünvanını təsdiqləyir.")
    @PostMapping("/change-email/confirm")
    public ResponseEntity<ApiResponse<UserResponse>> confirmEmailChange(
            @Valid @RequestBody az.fitnest.identity.dto.OtpVerifyRequest request) {
        Long userId = UserContext.getRequiredUserId();
        User updated = userService.confirmEmailChange(userId, request.otpSessionId(), request.otpCode());
        return ResponseEntity.ok(ApiResponse.success(UserResponseMapper.toResponse(updated)));
    }

    @Operation(summary = "Mobil nömrə dəyişmə sorğusu", description = "Yeni mobil nömrəyə OTP kodu göndərir. Cavabda otp_session_id qaytarılır ki, sonra /confirm endpoint-ində istifadə olunsun.")
    @PostMapping("/change-mobile/request")
    public ResponseEntity<ApiResponse<az.fitnest.identity.dto.OtpSendResponse>> requestMobileChange(
            @RequestParam String newMobile) {
        Long userId = UserContext.getRequiredUserId();
        az.fitnest.identity.dto.OtpSendResponse otpResponse = userService.requestMobileChange(userId, newMobile);
        return ResponseEntity.ok(ApiResponse.success(otpResponse));
    }

    @Operation(summary = "Mobil nömrə dəyişməsini təsdiqləyin", description = "OTP sessiya ID-si və OTP kodu vasitəsilə yeni mobil nömrəni təsdiqləyir.")
    @PostMapping("/change-mobile/confirm")
    public ResponseEntity<ApiResponse<UserResponse>> confirmMobileChange(
            @Valid @RequestBody az.fitnest.identity.dto.OtpVerifyRequest request) {
        Long userId = UserContext.getRequiredUserId();
        User updated = userService.confirmMobileChange(userId, request.otpSessionId(), request.otpCode());
        return ResponseEntity.ok(ApiResponse.success(UserResponseMapper.toResponse(updated)));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Şifrəni dəyişdirin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(
            @Valid @RequestBody az.fitnest.identity.dto.ChangePasswordRequest request,
            HttpServletRequest servletRequest) {
        Long userId = UserContext.getRequiredUserId();
        userService.changePassword(userId, request.oldPassword(), request.newPassword(), request.confirmNewPassword());
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "message", "Şifrə uğurla dəyişdirildi",
                "status", 200,
                "path", servletRequest.getRequestURI(),
                "timestamp", OffsetDateTime.now()
        )));
    }

    @PostMapping("/deactivate")
    @Operation(summary = "Hesabı deaktiv edin", description = "Autentifikasiya olunmuş istifadəçinin hesabını deaktiv edir. Bu, yumşaq silinmədir (status INACTIVE olur).")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Hesab uğurla deaktiv edildi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Autentifikasiya olunmayıb")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> deactivateAccount(HttpServletRequest request) {
        Long userId = UserContext.getRequiredUserId();
        userService.deactivateAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "message", "Hesab uğurla deaktiv edildi",
                "status", 200,
                "path", request.getRequestURI(),
                "timestamp", OffsetDateTime.now()
        )));
    }
}
