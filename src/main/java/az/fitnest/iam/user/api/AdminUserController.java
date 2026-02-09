package az.fitnest.iam.user.api;

import az.fitnest.iam.user.adapter.service.UserService;
import az.fitnest.iam.user.api.dto.mapper.UserResponseMapper;
import az.fitnest.iam.user.api.dto.response.UserResponse;
import az.fitnest.iam.user.domain.enums.RoleName;
import az.fitnest.iam.user.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Endpoints for admin user management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @PutMapping("/{userId}/role")
    @Operation(summary = "Change user role", description = "Promote or demote a user role. Requires ADMIN privileges.")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long userId,
            @RequestParam RoleName roleName) {
        User user = userService.updateUserRole(userId, roleName);
        return ResponseEntity.ok(UserResponseMapper.toResponse(user));
    }
}
