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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management Admin", description = "İstifadəçiləri idarə etmək və administrativ tənzimləmələri həyata keçirmək üçün ucluqlar.")
@SecurityRequirement(name = "bearerAuth")
public class UserAdminController {

    private final UserService userService;
    private final RateLimitAdminService rateLimitAdminService;

    @Operation(summary = "Bütün istifadəçiləri əldə edin", description = "Sistemdəki bütün istifadəçilərin siyahısını səhifələnmiş şəkildə qaytarır. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query) {
        // Parse query string for id, name, surname, email, mobile
        Long id = null;
        String name = null;
        String surname = null;
        String email = null;
        String mobile = null;
        if (query != null && !query.isBlank()) {
            String[] parts = query.split(";");
            for (String part : parts) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().toLowerCase();
                    String value = kv[1].trim();
                    switch (key) {
                        case "id":
                            try { id = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                            break;
                        case "name":
                            name = value;
                            break;
                        case "surname":
                            surname = value;
                            break;
                        case "email":
                            email = value;
                            break;
                        case "mobile":
                            mobile = value;
                            break;
                    }
                }
            }
        }
        return ResponseEntity.ok(userService.searchUsers(page, size, id, name, surname, email, mobile));
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
