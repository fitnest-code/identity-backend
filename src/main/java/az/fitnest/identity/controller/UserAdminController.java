package az.fitnest.identity.controller;

import az.fitnest.identity.dto.request.OtpRateLimitResetRequest;
import az.fitnest.identity.dto.request.OtpRateLimitUserResetRequest;
import az.fitnest.identity.dto.response.UserProfileDetailsResponse;
import az.fitnest.identity.service.UserProfileGrpcClient;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.service.impl.RateLimitAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import az.fitnest.identity.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "İstifadəçi İdarəetmə Admin", description = "İstifadəçilərin, rolların, limitlərin və axtarışın idarə olunması üçün admin endpointləri. Bütün endpointlər üçün ADMIN rolu tələb olunur.")
@SecurityRequirement(name = "bearerAuth")
public class UserAdminController {

    private final UserService userService;
    private final RateLimitAdminService rateLimitAdminService;
    private final UserProfileGrpcClient userProfileGrpcClient;
    private final DeviceService deviceService;

    @Operation(summary = "İstifadəçi rolunu dəyişdirin", description = "Müəyyən edilmiş istifadəçiyə yeni rol təyin edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String roleName) {
        userService.updateUserRole(userId, roleName);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "BÜTÜN istifadəçiləri deaktiv edin (Kritik)", description = "Sistemdəki bütün istifadəçi hesablarını deaktiv edir. Bu əməliyyat yalnız ADMIN tərəfindən həyata keçirilə bilər.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deactivateAllUsers() {
        userService.deactivateAllUsers();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İstifadəçi profil detallarını əldə edin", description = "İstifadəçi ID-si ilə profil detallarını qaytarır.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileDetailsResponse> getUserProfileDetails(@PathVariable Long userId) {
        var grpcResponse = userProfileGrpcClient.getUserProfileDetails(userId);
        UserProfileDetailsResponse response = UserProfileDetailsResponse.builder()
                .id(grpcResponse.getUserId())
                .registrationDate(grpcResponse.getRegistrationDate())
                .platform(grpcResponse.getPlatform())
                .phoneNumber(grpcResponse.getPhoneNumber())
                .email(grpcResponse.getEmail())
                .birthDate(java.time.LocalDate.parse(grpcResponse.getBirthDate()))
                .goal(grpcResponse.getGoal())
                .height(grpcResponse.getHeight())
                .weight(grpcResponse.getWeight())
                .bmiIndex(grpcResponse.getBmiIndex())
                .build();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Bütün rate limitləri sıfırla", description = "Verilən userId üçün bütün OTP və əlaqəli rate limitləri sıfırlayır. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rate-limit/reset")
    public ResponseEntity<Void> resetAllRateLimitsForUser(@RequestBody OtpRateLimitUserResetRequest request) {
        rateLimitAdminService.resetAllRateLimitsForUser(request.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reset OTP rate limits for a user", description = "Resets all OTP rate limits for the given user ID (all purposes). Admin only.")
    @ApiResponse(responseCode = "200", description = "OTP rate limits reset for user.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/otp-rate-limit/reset")
    public ResponseEntity<Void> resetOtpRateLimitForUser(@PathVariable Long userId) {
        rateLimitAdminService.resetAllRateLimitsForUser(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reset OTP rate limit for identifier and purpose", description = "Resets OTP rate limit for a specific identifier (mobile/email) and purpose. Admin only.")
    @ApiResponse(responseCode = "200", description = "OTP rate limit reset for identifier and purpose.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/otp-rate-limit/reset")
    public ResponseEntity<Void> resetOtpRateLimitForIdentifier(@RequestBody OtpRateLimitResetRequest request) {
        rateLimitAdminService.resetRateLimit(request.getPurpose(), request.getIdentifier());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reset all OTP rate limits for all users", description = "Resets all OTP rate limits for all users and all purposes. Admin only.")
    @ApiResponse(responseCode = "200", description = "All OTP rate limits reset.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/otp-rate-limit/reset-all")
    public ResponseEntity<Void> resetAllOtpRateLimits() {
        rateLimitAdminService.resetAllRateLimitsForAllUsers();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İstifadəçini bloklayın", description = "İstifadəçini bloklayır və bütün sessiyalarını sonlandırır. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/block")
    public ResponseEntity<Void> blockUser(@PathVariable Long userId) {
        userService.blockUser(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İstifadəçinin blokunu açın", description = "Bloklanmış istifadəçinin blokunu açır. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/unblock")
    public ResponseEntity<Void> unblockUser(@PathVariable Long userId) {
        userService.unblockUser(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İstifadəçi şifrəsini birbaşa sıfırlayın", description = "İstifadəçinin şifrəsini birbaşa sıfırlayır. Yalnız ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/password/reset")
    public ResponseEntity<Void> resetUserPassword(@PathVariable Long userId, @RequestBody az.fitnest.identity.dto.request.ResetUserPasswordRequest request) {
        userService.resetUserPasswordDirectly(userId, request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İstifadəçi rolunu dəyişdirin", description = "Müəyyən istifadəçiyə yeni rol təyin edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/change-role")
    public ResponseEntity<Void> changeUserRole(
            @PathVariable Long userId,
            @RequestBody az.fitnest.identity.dto.request.ChangeUserRoleRequest request) {
        userService.changeUserRole(userId, request.role());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Admin istifadəçi siyahısı", description = "Filtrlərlə istifadəçiləri siyahılar. roles parametri ilə rol üzrə filter edilə bilər. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<az.fitnest.identity.dto.PaginatedResponse<az.fitnest.identity.dto.response.AdminUserResponse>> getAdminUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long packageID,
            @RequestParam(required = false) Integer durationMonths,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String roles) {

        var result = userService.getAdminUsers(page, size, query, packageID, durationMonths, type, roles);
        return ResponseEntity.ok(az.fitnest.identity.dto.PaginatedResponse.of(result));
    }

    @Operation(summary = "İstifadəçini qalıcı olaraq silin", description = "İstifadəçini bütün token və sessiyaları ilə birlikdə DB-dən fiziki olaraq silir. Bu əməliyyat geri alına bilməz. Yalnız ADMIN rolu tələb olunur.")
    @ApiResponse(responseCode = "204", description = "İstifadəçi uğurla silindi")
    @ApiResponse(responseCode = "404", description = "İstifadəçi tapılmadı")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}/hard-delete")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable Long userId) {
        userService.hardDeleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İstifadəçinin cihaz limitini sıfırla", description = "İstifadəçinin cihaz ID-sini və dəyişmə limitini sıfırlayır. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/device-limit/reset")
    public ResponseEntity<Void> resetDeviceLimit(@PathVariable Long userId) {
        deviceService.resetDeviceLimit(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bütün istifadəçilərin cihaz limitini sıfırla", description = "Bütün istifadəçilərin cihaz ID-lərini və dəyişmə limitlərini sıfırlayır. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/device-limit/reset-all")
    public ResponseEntity<Void> resetAllDeviceLimits() {
        deviceService.resetAllDeviceLimits();
        return ResponseEntity.ok().build();
    }
}
