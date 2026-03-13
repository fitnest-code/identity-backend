package az.fitnest.identity.controller;

import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.service.impl.RateLimitAdminService;
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
@Tag(name = "User Management Admin", description = "Admin endpoints for managing users, roles, rate limits, and user search. All endpoints require ADMIN or SUPER_ADMIN roles.")
@SecurityRequirement(name = "bearerAuth")
public class UserAdminController {

    private final UserService userService;
    private final RateLimitAdminService rateLimitAdminService;

    @Operation(
        summary = "Get all users",
        description = "Returns a paginated list of all users in the system. Supports filtering by user ID, name, surname, email, mobile, package ID, and subscription duration (in months).",
        parameters = {
            @Parameter(name = "page", description = "Page number (0-based)", example = "0"),
            @Parameter(name = "size", description = "Page size", example = "10"),
            @Parameter(name = "query", description = "Filter string (e.g. 'name=John;surname=Doe')", example = "name=John;surname=Doe"),
            @Parameter(name = "packageID", description = "Package ID to filter users", example = "123"),
            @Parameter(name = "durationMonths", description = "Filter users by subscription duration in months", example = "12")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized. Authentication required.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden. Insufficient permissions.", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long packageID,
            @RequestParam(required = false) Integer durationMonths) {
        return ResponseEntity.ok(userService.searchUsersAdvanced(page, size, query, packageID, durationMonths));
    }

    @Operation(summary = "İstifadəçi rolunu dəyişdirin", description = "Müəyyən edilmiş istifadəçiyə yeni rol təyin edir. SUPER_ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String roleName) {
        userService.updateUserRole(userId, roleName);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Rate limit statusunu əldə edin", description = "İstifadəçinin OTP və digər limitlərinin cari vəziyyətini yoxlayır. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/rate-limit")
    public ResponseEntity<RateLimitAdminService.RateLimitStatus> getRateLimitStatus(
            @RequestParam OtpPurpose purpose,
            @RequestParam String phoneNumber) {
        return ResponseEntity.ok(rateLimitAdminService.getRateLimitStatus(purpose, phoneNumber));
    }

    @Operation(summary = "Rate limiti sıfırlayın", description = "İstifadəçinin OTP və ya digər limitlərini sıfırlayaraq yenidən cəhd etməsinə imkan yaradır. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rate-limit/reset")
    public ResponseEntity<Void> resetRateLimit(
            @RequestParam OtpPurpose purpose,
            @RequestParam String phoneNumber) {
        rateLimitAdminService.resetRateLimit(purpose, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "BÜTÜN istifadəçiləri deaktiv edin (Kritik)", description = "Sistemdəki bütün istifadəçi hesablarını deaktiv edir. Bu əməliyyat yalnız SUPER_ADMIN tərəfindən həyata keçirilə bilər.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deactivateAllUsers() {
        userService.deactivateAllUsers();
        return ResponseEntity.noContent().build();
    }

}
