package az.fitnest.identity.controller;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.service.AuthService;
import az.fitnest.identity.service.SocialAuthService;
import az.fitnest.identity.service.PasswordResetService;
import az.fitnest.identity.service.RegistrationService;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.dto.*;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.criteria.UserContext;
import az.fitnest.identity.model.entity.User;
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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "İstifadəçi autentifikasiyası, qeydiyyatı və şifrə idarəolunması üçün ucluqlar")
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final PasswordResetService passwordResetService;
    private final RegistrationService registrationService;
    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "İstifadəçi girişi", description = "İstifadəçini mobil nömrə və şifrə ilə autentifikasiya edir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Giriş uğurludur", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Yanlış məlumatlar", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"INVALID_CREDENTIALS\",\n    \"message\": \"Invalid credentials\",\n    \"status\": 401,\n    \"path\": \"/api/v1/auth/login\"\n  }\n}"))),
            @ApiResponse(responseCode = "400", description = "Yanlış sorğu formatı", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"VALIDATION_ERROR\",\n    \"message\": \"Validation failed\",\n    \"status\": 400,\n    \"path\": \"/api/v1/auth/login\"\n  }\n}"))),
            @ApiResponse(responseCode = "429", description = "Çox sayda giriş cəhdi (limit keçilib)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"LOGIN_RATE_LIMIT\",\n    \"message\": \"Too many login attempts. Please try again later.\",\n    \"status\": 429,\n    \"path\": \"/api/v1/auth/login\"\n  }\n}")))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Giriş tokenini yeniləyin")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token uğurla yeniləndi", content = @Content(schema = @Schema(implementation = RefreshResponse.class))),
            @ApiResponse(responseCode = "401", description = "Yanlış və ya vaxtı keçmiş yeniləmə tokeni")
    })
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "İstifadəçi çıxışı")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<az.fitnest.identity.dto.ApiResponse<Map<String, Object>>> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {
        if (authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        } else {
            throw new UnauthorizedException("Invalid Authorization header");
        }
        return ResponseEntity.ok(az.fitnest.identity.dto.ApiResponse.success(Map.of(
                "message", "Çıxış uğurla tamamlandı",
                "status", 200,
                "path", request.getRequestURI(),
                "timestamp", OffsetDateTime.now()
        )));
    }

    @PostMapping("/social/apple")
    @Operation(summary = "Apple ilə sosial giriş")
    public ResponseEntity<LoginResponse> socialLoginApple(@Valid @RequestBody az.fitnest.identity.dto.AppleSocialRequest request) {
        LoginResponse response = socialAuthService.socialLoginApple(request);
        HttpStatus status = response.user().setupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/social/google")
    @Operation(summary = "Google ilə sosial giriş")
    public ResponseEntity<LoginResponse> socialLoginGoogle(@Valid @RequestBody az.fitnest.identity.dto.GoogleSocialRequest request) {
        LoginResponse response = socialAuthService.socialLoginGoogle(request);
        HttpStatus status = response.user().setupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
