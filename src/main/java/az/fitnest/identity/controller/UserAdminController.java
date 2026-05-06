package az.fitnest.identity.controller;

import az.fitnest.identity.dto.response.AdminUserResponse;
import az.fitnest.identity.dto.PaginatedResponse;
import az.fitnest.identity.dto.response.UserProfileDetailsResponse;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.service.impl.RateLimitAdminService;
import az.fitnest.identity.service.UserProfileGrpcClient;
import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.dto.request.OtpRateLimitResetRequest;
import az.fitnest.identity.dto.request.OtpRateLimitUserResetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "İstifadəçi İdarəetmə Admin", description = "İstifadəçilərin, rolların, limitlərin və axtarışın idarə olunması üçün admin endpointləri. Bütün endpointlər üçün ADMIN rolu tələb olunur.")
@SecurityRequirement(name = "bearerAuth")
public class UserAdminController {

    private final UserService userService;
    private final RateLimitAdminService rateLimitAdminService;
    private final UserProfileGrpcClient userProfileGrpcClient;

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
}
