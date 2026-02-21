package az.fitnest.identity.controller;

import az.fitnest.identity.service.AuthService;
import az.fitnest.identity.service.SocialAuthService;
import az.fitnest.identity.service.PasswordResetService;
import az.fitnest.identity.service.RegistrationService;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.dto.*;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.constants.RoleName;
import az.fitnest.identity.entity.User;
import az.fitnest.identity.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication, registration, and admin user management")
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final PasswordResetService passwordResetService;
    private final RegistrationService registrationService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // --- Authentication & Registration ---

    @PostMapping("/auth/login")
    @Operation(summary = "User login", description = "Authenticates a user with mobile number and password. Returns access and refresh tokens.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(examples = @ExampleObject(value = "{\"error\": \"Invalid mobile number or password\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid request format")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("User login attempt for mobile: {}", request.getMobile());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/auth/refresh")
    @Operation(summary = "Refresh access token", description = "Generates a new access token using a valid refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(schema = @Schema(implementation = RefreshResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/auth/register")
    @Operation(summary = "Initiate registration", description = "Starts the registration process by collecting and validating the mobile number. An OTP will be sent.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP sent successfully", content = @Content(schema = @Schema(implementation = OtpSendResponse.class))),
            @ApiResponse(responseCode = "409", description = "Mobile number already registered")
    })
    public ResponseEntity<OtpSendResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(registrationService.startRegistration(request));
    }

    @PostMapping("/auth/register/complete")
    @Operation(summary = "Complete registration", description = "Completes the registration process using the token received after OTP verification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid registration token")
    })
    public ResponseEntity<LoginResponse> registerComplete(@Valid @RequestBody RegisterCompleteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.completeRegistration(request));
    }

    @PostMapping("/auth/social/apple")
    @Operation(summary = "Apple social login", description = "Authenticates or registers a user using Apple ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "201", description = "New user registered via Apple", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid Apple identity token")
    })
    public ResponseEntity<LoginResponse> socialLoginApple(@Valid @RequestBody AppleSocialRequest request) {
        LoginResponse response = socialAuthService.socialLoginApple(request);
        HttpStatus status = response.getUser().isSetupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/auth/social/google")
    @Operation(summary = "Google social login", description = "Authenticates or registers a user using Google account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "201", description = "New user registered via Google", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid Google identity token")
    })
    public ResponseEntity<LoginResponse> socialLoginGoogle(@Valid @RequestBody GoogleSocialRequest request) {
        LoginResponse response = socialAuthService.socialLoginGoogle(request);
        HttpStatus status = response.getUser().isSetupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/auth/forgot-password")
    @Operation(summary = "Forgot password", description = "Initiates password reset process for the given mobile number.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP for password reset sent"),
            @ApiResponse(responseCode = "404", description = "User not found with this mobile number")
    })
    public ResponseEntity<OtpSendResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.forgotPassword(request));
    }

    @PostMapping("/auth/reset-password")
    @Operation(summary = "Reset password", description = "Resets user password using the reset token and new password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid reset token or password policy violation")
    })
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }

    @PostMapping("/auth/change-password")
    @Operation(summary = "Change password", description = "Changes password for an authenticated user. Requires current password verification.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid current password or session"),
            @ApiResponse(responseCode = "400", description = "New password does not meet requirements")
    })
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword(), request.getConfirmNewPassword());
        return ResponseEntity.ok().build();
    }

    // --- Admin User Management ---

    @GetMapping("/admin/users")
    @Operation(summary = "Get all users (Admin)", description = "Returns a paginated list of all users. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return ResponseEntity.ok(userService.getAllUsersMapped(page, page_size));
    }

    @GetMapping("/admin/users/{userId}")
    @Operation(summary = "Get user by ID (Admin)", description = "Returns detailed user profile by ID. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(UserResponseMapper.toResponse(userService.getUserById(userId)));
    }

    @PutMapping("/admin/users/{userId}/role")
    @Operation(summary = "Change user role (Admin)", description = "Updates the role of a user. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long userId,
            @RequestParam RoleName roleName) {
        User user = userService.updateUserRole(userId, roleName);
        return ResponseEntity.ok(UserResponseMapper.toResponse(user));
    }

    @DeleteMapping("/admin/users/{userId}")
    @Operation(summary = "Delete user (Admin)", description = "Permanently deletes a user account. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        userService.deleteUser(userId, reason);
        return ResponseEntity.noContent().build();
    }
}
