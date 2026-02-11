package az.fitnest.identity.controller;

import az.fitnest.identity.service.UserService;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.constants.RoleName;
import az.fitnest.identity.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Endpoints for admin user management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users", description = "Returns a paginated list of all users. Requires ADMIN privileges.")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        Page<User> userPage = userService.getAllUsers(PageRequest.of(page - 1, page_size));
        return ResponseEntity.ok(userPage.map(UserResponseMapper::toResponse));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Returns user details by ID. Requires ADMIN privileges.")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserResponseMapper.toResponse(user));
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Change user role", description = "Promote or demote a user role. Requires ADMIN privileges.")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long userId,
            @RequestParam RoleName roleName) {
        User user = userService.updateUserRole(userId, roleName);
        return ResponseEntity.ok(UserResponseMapper.toResponse(user));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Deletes a user account. Requires ADMIN privileges.")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        userService.deleteUser(userId, reason);
        return ResponseEntity.noContent().build();
    }
}
