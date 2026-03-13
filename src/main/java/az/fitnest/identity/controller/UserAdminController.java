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
@Tag(name = "İstifadəçi İdarəetmə Admin", description = "İstifadəçilərin, rolların, limitlərin və axtarışın idarə olunması üçün admin endpointləri. Bütün endpointlər üçün ADMIN və ya SUPER_ADMIN rolu tələb olunur.")
@SecurityRequirement(name = "bearerAuth")
public class UserAdminController {

    private final UserService userService;
    private final RateLimitAdminService rateLimitAdminService;

    @Operation(
        summary = "Bütün istifadəçiləri əldə edin",
        description = "Sistemdəki bütün istifadəçilərin səhifələnmiş siyahısını qaytarır. İstifadəçi ID-si, ad, soyad, email, mobil, status və abunə statusu üzrə filtrləmə dəstəklənir.",
        parameters = {
            @Parameter(name = "page", description = "Səhifə nömrəsi (0-dan başlayır)", example = "0"),
            @Parameter(name = "size", description = "Səhifə ölçüsü", example = "10"),
            @Parameter(name = "query", description = "Filtr sətiri. Adi mətn (məsələn, 'kamal') və ya açar-dəyər cütləri (məsələn, 'name=Kamal;surname=Aliyev;email=kamal@example.com;mobile=0501234567') dəstəklənir.", example = "kamal və ya name=Kamal;surname=Aliyev;email=kamal@example.com;mobile=0501234567"),
            @Parameter(name = "packageID", description = "İstifadəçiləri paket ID-sinə görə filtr edin", example = "5"),
            @Parameter(name = "durationMonths", description = "Abunə müddətinə görə filtr edin (aylarla)", example = "5")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "İstifadəçilər uğurla əldə edildi", content = @Content(schema = @Schema(implementation = az.fitnest.identity.dto.AdminUserResponse.class))),
        @ApiResponse(responseCode = "401", description = "İcazə verilmir. Giriş tələb olunur.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Qadağandır. Yetərli səlahiyyət yoxdur.", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<az.fitnest.identity.dto.AdminUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long packageID,
            @RequestParam(required = false) Integer durationMonths) {
        return ResponseEntity.ok(userService.getAdminUsers(page, size, query, packageID, durationMonths));
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
