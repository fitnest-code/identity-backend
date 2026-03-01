package az.fitnest.identity.controller;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.PaginatedResponse;
import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management Admin", description = "İstifadəçiləri idarə etmək üçün administrativ ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class UserAdminController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Bütün istifadəçiləri əldə edin (Admin)", description = "Bütün istifadəçilərin səhifələnmiş siyahısını qaytarır. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "İstifadəçilər uğurla əldə edildi", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Kifayət qədər icazə yoxdur")
    })
    public ResponseEntity<PaginatedResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return ResponseEntity.ok(PaginatedResponse.of(userService.getAllUsersMapped(page, page_size)));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "İstifadəçini ID vasitəsilə əldə edin (Admin)", description = "ID vasitəsilə istifadəçinin ətraflı profilini qaytarır. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "İstifadəçi təfərrüatları əldə edildi"),
            @ApiResponse(responseCode = "404", description = "İstifadəçi tapılmadı")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(UserResponseMapper.toResponse(userService.getUserById(userId)));
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "İstifadəçi rolunu dəyişdirin (Admin)", description = "İstifadəçinin rolunu yeniləyir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol uğurla yeniləndi"),
            @ApiResponse(responseCode = "404", description = "İstifadəçi tapılmadı")
    })
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long userId,
            @RequestParam String roleName) {
        User user = userService.updateUserRole(userId, roleName);
        return ResponseEntity.ok(UserResponseMapper.toResponse(user));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "İstifadəçini silin (Admin)", description = "İstifadəçi hesabını həmişəlik silir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "İstifadəçi uğurla silindi"),
            @ApiResponse(responseCode = "404", description = "İstifadəçi tapılmadı")
    })
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        userService.deleteUser(userId, reason);
        return ResponseEntity.noContent().build();
    }
}
