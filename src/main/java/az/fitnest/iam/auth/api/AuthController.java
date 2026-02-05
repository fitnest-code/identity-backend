package az.fitnest.iam.auth.api;

import az.fitnest.iam.auth.adapter.service.AuthService;
import az.fitnest.iam.auth.adapter.service.SocialAuthService;
import az.fitnest.iam.auth.adapter.service.PasswordResetService;
import az.fitnest.iam.auth.adapter.service.RegistrationService;
import az.fitnest.iam.auth.api.dto.request.AppleSocialRequest;
import az.fitnest.iam.auth.api.dto.request.GoogleSocialRequest;
import az.fitnest.iam.auth.api.dto.request.LoginRequest;
import az.fitnest.iam.auth.api.dto.request.ForgotPasswordRequest;
import az.fitnest.iam.auth.api.dto.request.RefreshRequest;
import az.fitnest.iam.auth.api.dto.request.RegisterCompleteRequest;
import az.fitnest.iam.auth.api.dto.request.RegisterRequest;
import az.fitnest.iam.auth.api.dto.request.ResetPasswordRequest;
import az.fitnest.iam.auth.api.dto.response.ForgotPasswordResponse;
import az.fitnest.iam.auth.api.dto.response.LoginResponse;
import az.fitnest.iam.auth.api.dto.response.RefreshResponse;
import az.fitnest.iam.auth.api.dto.response.ResetPasswordResponse;
import az.fitnest.iam.auth.api.dto.response.VerifyOtpForPasswordResetResponse;
import az.fitnest.iam.otp.api.dto.response.OtpSendResponse;
import az.fitnest.iam.otp.api.dto.request.OtpVerifyRequest;
import az.fitnest.iam.shared.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication, token management, and registration")
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final PasswordResetService passwordResetService;
    private final RegistrationService registrationService;

    @Operation(
            summary = "User login",
            description = "Authenticates a user with mobile number and password. Returns JWT access and refresh tokens. " +
                    "Account will be locked after 5 failed login attempts."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generates new access and refresh tokens using a valid refresh token. " +
                    "The old tokens are invalidated upon successful refresh."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = RefreshResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Initiate registration",
            description = "Starts the registration process by collecting user details and sending a 4-digit OTP code to the provided email. " +
                    "Registration is only completed after the OTP is verified in the next step."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP sent and registration initiated",
                    content = @Content(schema = @Schema(implementation = OtpSendResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or email already in use",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many OTP requests",
                    content = @Content
            )
    })
    @PostMapping("/register")
    public ResponseEntity<OtpSendResponse> register(@Valid @RequestBody RegisterRequest request) {
        OtpSendResponse response = registrationService.startRegistration(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Complete registration",
            description = "Verifies the OTP code sent in the previous step and creates the user account. " +
                    "Returns JWT access and refresh tokens upon successful verification."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Registration completed successfully and user logged in",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data, wrong OTP, or expired session",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid OTP code",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already registered",
                    content = @Content
            )
    })
    @PostMapping("/register/complete")
    public ResponseEntity<LoginResponse> registerComplete(@Valid @RequestBody RegisterCompleteRequest request) {
        LoginResponse response = registrationService.completeRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Apple social login",
            description = "Authenticates a user with Apple ID. Creates a new account if user doesn't exist. " +
                    "Returns 200 OK for existing accounts, 201 Created for new accounts."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful (existing account)",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "201",
                    description = "Account created and logged in",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid Apple token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Account conflict: email already registered",
                    content = @Content
            )
    })
    @PostMapping("/social/apple")
    public ResponseEntity<LoginResponse> socialLoginApple(@Valid @RequestBody AppleSocialRequest request) {
        LoginResponse response = socialAuthService.socialLoginApple(request);
        boolean isNewAccount = response.getUser().isSetupRequired();
        return ResponseEntity.status(isNewAccount ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }

    @Operation(
            summary = "Google social login",
            description = "Authenticates a user with Google account. Creates a new account if user doesn't exist. " +
                    "Returns 200 OK for existing accounts, 201 Created for new accounts."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful (existing account)",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "201",
                    description = "Account created and logged in",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid Google token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Account conflict: email already registered",
                    content = @Content
            )
    })
    @PostMapping("/social/google")
    public ResponseEntity<LoginResponse> socialLoginGoogle(@Valid @RequestBody GoogleSocialRequest request) {
        LoginResponse response = socialAuthService.socialLoginGoogle(request);
        boolean isNewAccount = response.getUser().isSetupRequired();
        return ResponseEntity.status(isNewAccount ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }

    @Operation(
            summary = "Request password reset",
            description = "Sends an OTP code to the user's email for password reset. " +
                    "Returns a generic success message to prevent email enumeration."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully (if email exists)",
                    content = @Content(schema = @Schema(implementation = ForgotPasswordResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many requests - rate limited",
                    content = @Content
            )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = passwordResetService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Verify OTP for password reset",
            description = "Verifies the OTP code sent for password reset and returns a reset token. " +
                    "The reset token is required to reset the password."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP verified successfully",
                    content = @Content(schema = @Schema(implementation = VerifyOtpForPasswordResetResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired OTP code",
                    content = @Content
            )
    })
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<VerifyOtpForPasswordResetResponse> verifyOtpForPasswordReset(
            @Valid @RequestBody OtpVerifyRequest request) {
        VerifyOtpForPasswordResetResponse response = passwordResetService.verifyOtpForPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reset password",
            description = "Resets the user's password using a valid reset token obtained from OTP verification. " +
                    "The reset token is consumed after successful password reset."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = ResetPasswordResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation failed",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired reset token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content
            )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = passwordResetService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        return authorization.substring(7).trim();
    }
}
