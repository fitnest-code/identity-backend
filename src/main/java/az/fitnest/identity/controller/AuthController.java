package az.fitnest.identity.controller;

import az.fitnest.identity.dto.request.AppleSocialRequest;
import az.fitnest.identity.dto.request.GoogleSocialRequest;
import az.fitnest.identity.dto.request.LoginRequest;
import az.fitnest.identity.dto.request.RefreshRequest;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.dto.response.LoginResult;
import az.fitnest.identity.dto.response.OtpSendResponse;
import az.fitnest.identity.dto.response.RefreshResponse;
import az.fitnest.identity.dto.response.SuccessResponse;
import az.fitnest.identity.service.AuthService;
import az.fitnest.identity.service.PasswordResetService;
import az.fitnest.identity.service.RegistrationService;
import az.fitnest.identity.service.SocialAuthService;
import az.fitnest.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autentifikasiya", description = "İstifadəçi autentifikasiyası, giriş, çıxış, token yeniləmə və şifrə idarəetməsi üçün endpointlər.")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final PasswordResetService passwordResetService;
    private final RegistrationService registrationService;
    private final UserService userService;
    private final MessageSource messageSource;

    @PostMapping("/login")
    @Operation(summary = "İstifadəçi girişi", description = "İstifadəçini mobil nömrə və şifrə ilə autentifikasiya edir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Giriş uğurludur və ya əlavə addım tələb olunur",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(oneOf = {LoginResponse.class, OtpSendResponse.class}))),
            @ApiResponse(responseCode = "401", description = "Yanlış məlumatlar", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"INVALID_CREDENTIALS\",\n    \"message\": \"Yanlış giriş məlumatları\",\n    \"status\": 401,\n    \"path\": \"/api/v1/auth/login\"\n  }\n}"))),
            @ApiResponse(responseCode = "400", description = "Yanlış sorğu formatı", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"VALIDATION_ERROR\",\n    \"message\": \"Doğrulama uğursuz oldu\",\n    \"status\": 400,\n    \"path\": \"/api/v1/auth/login\"\n  }\n}"))),
            @ApiResponse(responseCode = "429", description = "Çox sayda giriş cəhdi (limit keçilib)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"LOGIN_RATE_LIMIT\",\n    \"message\": \"Çox sayda giriş cəhdi. Zəhmət olmasa bir az sonra yenidən cəhd edin.\",\n    \"status\": 429,\n    \"path\": \"/api/v1/auth/login\"\n  }\n}")))
    })
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        LoginResult result = authService.login(request);
        return ResponseEntity.ok(result.payload());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Giriş tokenini yeniləyin", description = "Giriş tokenini yeniləyir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token uğurla yeniləndi", content = @Content(schema = @Schema(implementation = RefreshResponse.class))),
            @ApiResponse(responseCode = "401", description = "Yanlış və ya vaxtı keçmiş yeniləmə tokeni")
    })
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "İstifadəçi çıxışı", description = "İstifadəçi sessiyasını sonlandırır.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<SuccessResponse>> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {
        authService.logoutFromHeader(authHeader);
        return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(
                SuccessResponse.of(getMessage("success.auth.logout"), request.getRequestURI())
        ));
    }

    @PostMapping("/social/apple")
    @Operation(summary = "Apple ilə sosial giriş")
    public ResponseEntity<LoginResponse> socialLoginApple(
            @Valid @RequestBody AppleSocialRequest request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        LoginResponse response = socialAuthService.socialLoginApple(request);
        HttpStatus status = response.user().setupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/social/google")
    @Operation(summary = "Google ilə sosial giriş", security = @SecurityRequirement(name = "none"))
    public ResponseEntity<LoginResponse> socialLoginGoogle(
            @Valid @RequestBody GoogleSocialRequest request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received Google social login request");
        LoginResponse response = socialAuthService.socialLoginGoogle(request);
        HttpStatus status = response.user().setupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        log.info("Google social login successful for user: {}", response.user().userId());
        return ResponseEntity.status(status).body(response);
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}
