package az.fitnest.identity.controller;

import az.fitnest.identity.service.AuthService;
import az.fitnest.identity.service.SocialAuthService;
import az.fitnest.identity.service.PasswordResetService;
import az.fitnest.identity.service.RegistrationService;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.dto.*;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.criteria.UserContext;
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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication, registration, and password management")
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final PasswordResetService passwordResetService;
    private final RegistrationService registrationService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates a user with mobile number and password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Invalid request format")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("User login attempt for mobile: {}", request.getMobile());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(schema = @Schema(implementation = RefreshResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout/all")
    @Operation(summary = "Logout from all sessions")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logoutAll() {
        Long userId = az.fitnest.identity.criteria.UserContext.getRequiredUserId();
        authService.logoutAll(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    @Operation(summary = "Initiate registration")
    public ResponseEntity<OtpSendResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(registrationService.startRegistration(request));
    }

    @PostMapping("/register/complete")
    @Operation(summary = "Complete registration")
    public ResponseEntity<LoginResponse> registerComplete(@Valid @RequestBody RegisterCompleteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.completeRegistration(request));
    }

    @PostMapping("/social/apple")
    @Operation(summary = "Apple social login")
    public ResponseEntity<LoginResponse> socialLoginApple(@Valid @RequestBody AppleSocialRequest request) {
        LoginResponse response = socialAuthService.socialLoginApple(request);
        HttpStatus status = response.getUser().isSetupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/social/google")
    @Operation(summary = "Google social login")
    public ResponseEntity<LoginResponse> socialLoginGoogle(@Valid @RequestBody GoogleSocialRequest request) {
        LoginResponse response = socialAuthService.socialLoginGoogle(request);
        HttpStatus status = response.getUser().isSetupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password")
    public ResponseEntity<OtpSendResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.getRequiredUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword(), request.getConfirmNewPassword());
        return ResponseEntity.ok().build();
    }
}
