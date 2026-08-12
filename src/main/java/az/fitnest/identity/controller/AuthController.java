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
import az.fitnest.identity.dto.request.LoginRequestV2;
import az.fitnest.identity.dto.request.GoogleSocialRequestV2;
import az.fitnest.identity.dto.request.AppleSocialRequestV2;
import az.fitnest.identity.dto.request.RegisterCompleteRequestV2;
import az.fitnest.identity.dto.request.LoginCheckRequestV3;
import az.fitnest.identity.dto.request.LoginRequestV3;
import az.fitnest.identity.dto.request.LoginVerifyRequestV3;
import az.fitnest.identity.dto.request.RegisterCompleteRequestV3;
import az.fitnest.identity.dto.request.RegisterRequestV3;
import az.fitnest.identity.dto.response.ApiResponse;
import az.fitnest.identity.dto.response.LoginEligibilityResponse;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autentifikasiya", description = "İstifadəçi autentifikasiyası (V1, V2, V3), giriş, çıxış, token yeniləmə və şifrə idarəetməsi üçün vahid controller.")
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final PasswordResetService passwordResetService;
    private final RegistrationService registrationService;
    private final UserService userService;
    private final MessageSource messageSource;

    // ==========================================
    // V1 Auth Endpoints
    // ==========================================

    @PostMapping("/api/v1/auth/login")
    @Operation(summary = "İstifadəçi girişi V1", description = "İstifadəçini mobil nömrə və şifrə ilə autentifikasiya edir.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Giriş uğurludur və ya əlavə addım tələb olunur",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(oneOf = {LoginResponse.class, OtpSendResponse.class}))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Yanlış məlumatlar", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"INVALID_CREDENTIALS\",\n    \"message\": \"Yanlış giriş məlumatları\",\n    \"status\": 401,\n    \"path\": \"/api/v1/auth/login\"\n  }\n}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Yanlış sorğu formatı", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"VALIDATION_ERROR\",\n    \"message\": \"Doğrulama uğursuz oldu\",\n    \"status\": 400,\n    \"path\": \"/api/v1/auth/login\"\n  }\n}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Çox sayda giriş cəhdi (limit keçilib)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"LOGIN_RATE_LIMIT\",\n    \"message\": \"Çox sayda giriş cəhdi. Zəhmət olmasa bir az sonra yenidən cəhd edin.\\\",\n    \"status\": 429,\n    \"path\": \"/api/v1/auth/login\"\n  }\n}")))
    })
    public ResponseEntity<?> loginV1(
            @Valid @RequestBody LoginRequest request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        LoginResult result = authService.login(request);
        return ResponseEntity.ok(result.payload());
    }

    @PostMapping({"/api/v1/auth/refresh", "/api/v2/auth/refresh", "/api/v3/auth/refresh"})
    @Operation(summary = "Giriş tokenini yeniləyin", description = "Giriş tokenini yeniləyir.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token uğurla yeniləndi", content = @Content(schema = @Schema(implementation = RefreshResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Yanlış və ya vaxtı keçmiş yeniləmə tokeni")
    })
    public ResponseEntity<RefreshResponse> refreshV1(
            @Valid @RequestBody RefreshRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "X-Device-Type", required = false) String xDeviceType,
            @RequestHeader(value = "X-Platform", required = false) String xPlatform) {
        return ResponseEntity.ok(authService.refresh(request, userAgent, xDeviceType, xPlatform));
    }

    @PostMapping("/api/v1/auth/logout")
    @Operation(summary = "İstifadəçi çıxışı", description = "İstifadəçi çıxışını həyata keçirir.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<SuccessResponse>> logoutV1(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {
        authService.logoutFromHeader(authHeader);
        return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(
                SuccessResponse.of(getMessage("success.auth.logout"), request.getRequestURI())
        ));
    }

    @PostMapping("/api/v1/auth/social/apple")
    @Operation(summary = "Apple ilə sosial giriş")
    public ResponseEntity<LoginResponse> socialLoginAppleV1(
            @Valid @RequestBody AppleSocialRequest request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        LoginResponse response = socialAuthService.socialLoginApple(request);
        HttpStatus status = response.user().setupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/api/v1/auth/social/google")
    @Operation(summary = "Google ilə sosial giriş", security = @SecurityRequirement(name = "none"))
    public ResponseEntity<LoginResponse> socialLoginGoogleV1(
            @Valid @RequestBody GoogleSocialRequest request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received Google social login request");
        LoginResponse response = socialAuthService.socialLoginGoogle(request);
        HttpStatus status = response.user().setupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        log.info("Google social login successful for user: {}", response.user().userId());
        return ResponseEntity.status(status).body(response);
    }

    // ==========================================
    // V2 Auth Endpoints
    // ==========================================

    @PostMapping("/api/v2/auth/login")
    @Operation(summary = "İstifadəçi girişi V2", description = "İstifadəçini mobil nömrə, şifrə, cihaz ID-si və cihaz tipi ilə autentifikasiya edir.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Giriş uğurludur və ya əlavə addım tələb olunur",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(oneOf = {LoginResponse.class, OtpSendResponse.class}))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Yanlış məlumatlar və ya cihaz uyğunsuzluğu", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"INVALID_CREDENTIALS\",\n    \"message\": \"Yanlış giriş məlumatları\",\n    \"status\": 401,\n    \"path\": \"/api/v2/auth/login\"\n  }\n}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Yanlış sorğu formatı", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"VALIDATION_ERROR\",\n    \"message\": \"Doğrulama uğursuz oldu\",\n    \"status\": 400,\n    \"path\": \"/api/v2/auth/login\"\n  }\n}")))
    })
    public ResponseEntity<?> loginV2(
            @Valid @RequestBody LoginRequestV2 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received Login V2 request for mobile number: {}", request.mobile());
        LoginResult result = authService.loginV2(request);
        return ResponseEntity.ok(result.payload());
    }

    @PostMapping("/api/v2/auth/social/google")
    @Operation(summary = "Google ilə sosial giriş V2")
    public ResponseEntity<LoginResponse> socialLoginGoogleV2(
            @Valid @RequestBody GoogleSocialRequestV2 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received Google V2 social login request");
        LoginResponse response = socialAuthService.socialLoginGoogleV2(request);
        HttpStatus status = response.user().setupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/api/v2/auth/social/apple")
    @Operation(summary = "Apple ilə sosial giriş V2")
    public ResponseEntity<LoginResponse> socialLoginAppleV2(
            @Valid @RequestBody AppleSocialRequestV2 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received Apple V2 social login request");
        LoginResponse response = socialAuthService.socialLoginAppleV2(request);
        HttpStatus status = response.user().setupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/api/v2/auth/registration/register/complete")
    @Operation(summary = "Qeydiyyatı tamamla V2")
    public ResponseEntity<LoginResponse> completeRegistrationV2(
            @Valid @RequestBody RegisterCompleteRequestV2 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received Registration Complete V2 request");
        LoginResponse response = registrationService.completeRegistrationV2(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v2/auth/social/google/login/add-number/otp/request")
    @Operation(summary = "Google girişi nömrə əlavə etmək üçün OTP sorğusu")
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<OtpSendResponse>> requestAddNumberOtpGoogle(
            @Valid @RequestBody az.fitnest.identity.dto.request.AddNumberOtpRequest request,
            @RequestParam(required = false) String lang) {
        Long userId = az.fitnest.identity.util.UserContext.getRequiredUserId();
        log.info("Received Add Number OTP request for Google social user ID: {}", userId);
        OtpSendResponse response = socialAuthService.requestAddNumberOtpGoogle(userId, request);
        return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(response));
    }

    @PostMapping("/api/v2/auth/social/apple/login/add-number/otp/request")
    @Operation(summary = "Apple girişi nömrə əlavə etmək üçün OTP sorğusu")
    public ResponseEntity<az.fitnest.identity.dto.response.ApiResponse<OtpSendResponse>> requestAddNumberOtpApple(
            @Valid @RequestBody az.fitnest.identity.dto.request.AddNumberOtpRequest request,
            @RequestParam(required = false) String lang) {
        Long userId = az.fitnest.identity.util.UserContext.getRequiredUserId();
        log.info("Received Add Number OTP request for Apple social user ID: {}", userId);
        OtpSendResponse response = socialAuthService.requestAddNumberOtpApple(userId, request);
        return ResponseEntity.ok(az.fitnest.identity.dto.response.ApiResponse.success(response));
    }

    @PostMapping("/api/v2/auth/social/google/login/add-number/otp/verify")
    @Operation(summary = "Google girişi nömrə əlavə etmək üçün OTP təsdiqi")
    public ResponseEntity<LoginResponse> verifyAddNumberOtpGoogle(
            @Valid @RequestBody az.fitnest.identity.dto.request.AddNumberOtpVerifyRequest request,
            @RequestParam(required = false) String lang) {
        Long userId = az.fitnest.identity.util.UserContext.getRequiredUserId();
        log.info("Received Add Number OTP verification request for Google social user ID: {}", userId);
        LoginResponse response = socialAuthService.verifyAddNumberOtpGoogle(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v2/auth/social/apple/login/add-number/otp/verify")
    @Operation(summary = "Apple girişi nömrə əlavə etmək üçün OTP təsdiqi")
    public ResponseEntity<LoginResponse> verifyAddNumberOtpApple(
            @Valid @RequestBody az.fitnest.identity.dto.request.AddNumberOtpVerifyRequest request,
            @RequestParam(required = false) String lang) {
        Long userId = az.fitnest.identity.util.UserContext.getRequiredUserId();
        log.info("Received Add Number OTP verification request for Apple social user ID: {}", userId);
        LoginResponse response = socialAuthService.verifyAddNumberOtpApple(userId, request);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // V3 Auth Endpoints
    // ==========================================

    @PostMapping("/api/v3/auth/registration/register")
    @Operation(summary = "Qeydiyyatı başladın V3 (şifrəsiz)", description = "Mobil nömrə ilə qeydiyyatı başladır və OTP göndərir. Şifrə tələb olunmur.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP uğurla göndərildi",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OtpSendResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Bu nömrə artıq qeydiyyatdan keçib",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":{\"code\":\"DUPLICATE_MOBILE\",\"message\":\"Bu nömrə artıq qeydiyyatdan keçib\",\"status\":409}}")))
    })
    public ResponseEntity<ApiResponse<OtpSendResponse>> registerV3(
            @Valid @RequestBody RegisterRequestV3 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received V3 registration request for mobile: {}", request.mobile());
        return ResponseEntity.ok(ApiResponse.success(registrationService.startRegistrationV3(request)));
    }

    @PostMapping("/api/v3/auth/registration/register/complete")
    @Operation(summary = "Qeydiyyatı tamamlayın V3 (şifrəsiz)", description = "OTP doğrulamasından sonra ad və soyad ilə qeydiyyatı tamamlayır. Şifrə tələb olunmur.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Qeydiyyat uğurla tamamlandı",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Yanlış sorğu formatı")
    })
    public ResponseEntity<LoginResponse> completeRegistrationV3(
            @Valid @RequestBody RegisterCompleteRequestV3 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received V3 registration complete request");
        LoginResponse response = registrationService.completeRegistrationV3(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v3/auth/login")
    @Operation(summary = "Girişi başladın V3 (şifrəsiz)", description = "Mobil nömrə ilə giriş OTP-si göndərir. Şifrə tələb olunmur.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP uğurla göndərildi",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OtpSendResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "İstifadəçi tapılmadı və ya hesab bloklanıb",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":{\"code\":\"INVALID_CREDENTIALS\",\"message\":\"Yanlış giriş məlumatları\",\"status\":401}}")))
    })
    public ResponseEntity<ApiResponse<OtpSendResponse>> loginV3(
            @Valid @RequestBody LoginRequestV3 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received V3 login request for mobile: {}", request.mobile());
        OtpSendResponse response = authService.startLoginV3(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/api/v3/auth/login/verify")
    @Operation(summary = "Girişi doğrulayın V3", description = "OTP kodu və cihaz məlumatları ilə girişi doğrulayır. iOS/Android üçün cihaz ID tələb olunur. Cihaz dəyişikliyi limiti 3-dür.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Giriş uğurludur",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "OTP yanlışdır, cihaz limiti keçilib, və ya cihaz ID tələb olunur",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":{\"code\":\"INVALID_CREDENTIALS\",\"message\":\"Limiti keçmisiniz\",\"status\":401}}")))
    })
    public ResponseEntity<LoginResponse> verifyLoginV3(
            @Valid @RequestBody LoginVerifyRequestV3 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received V3 login verify request");
        LoginResponse response = authService.verifyLoginV3(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v3/auth/login/check")
    @Operation(summary = "Giriş uyğunluğunu yoxlayın V3", description = "İstifadəçinin girişə uyğun olub-olmadığını yoxlayır. Hesab statusunu (bloklanma, silinmə, kilidlənmə) və cihaz limitini yoxlayır. OTP göndərilmir, token yaradılmır.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "İstifadəçi girişə uyğundur",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginEligibilityResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "İstifadəçi tapılmadı",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":{\"code\":\"INVALID_CREDENTIALS\",\"message\":\"Yanlış giriş məlumatları\",\"status\":401}}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Hesab bloklanıb, silinib, kilidlənib və ya cihaz limiti keçilib",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":{\"code\":\"error.auth.account_blocked\",\"message\":\"Hesabınız bloklanıb\",\"status\":403}}")))
    })
    public ResponseEntity<LoginEligibilityResponse> checkLoginEligibilityV3(
            @Valid @RequestBody LoginCheckRequestV3 request,
            @RequestParam(required = false) String lang) {
        log.info("Received V3 login eligibility check request for mobile: {}", request.mobile());
        LoginEligibilityResponse response = authService.checkLoginEligibility(request);
        return ResponseEntity.ok(response);
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}
