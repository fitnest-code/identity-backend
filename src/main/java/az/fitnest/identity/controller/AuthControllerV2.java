package az.fitnest.identity.controller;

import az.fitnest.identity.dto.request.LoginRequestV2;
import az.fitnest.identity.dto.request.GoogleSocialRequestV2;
import az.fitnest.identity.dto.request.AppleSocialRequestV2;
import az.fitnest.identity.dto.request.RegisterCompleteRequestV2;
import az.fitnest.identity.dto.response.LoginResult;
import az.fitnest.identity.dto.response.OtpSendResponse;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.service.AuthService;
import az.fitnest.identity.service.SocialAuthService;
import az.fitnest.identity.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autentifikasiya V2", description = "İstifadəçi autentifikasiyası V2, cihaz bağlama dəstəyi və cihaz tipi ilə.")
@Slf4j
@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
public class AuthControllerV2 {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    @Operation(summary = "İstifadəçi girişi V2", description = "İstifadəçini mobil nömrə, şifrə, cihaz ID-si və cihaz tipi ilə autentifikasiya edir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Giriş uğurludur və ya əlavə addım tələb olunur",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(oneOf = {LoginResponse.class, OtpSendResponse.class}))),
            @ApiResponse(responseCode = "401", description = "Yanlış məlumatlar və ya cihaz uyğunsuzluğu", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"INVALID_CREDENTIALS\",\n    \"message\": \"Yanlış giriş məlumatları\",\n    \"status\": 401,\n    \"path\": \"/api/v2/auth/login\"\n  }\n}"))),
            @ApiResponse(responseCode = "400", description = "Yanlış sorğu formatı", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\n  \"error\": {\n    \"code\": \"VALIDATION_ERROR\",\n    \"message\": \"Doğrulama uğursuz oldu\",\n    \"status\": 400,\n    \"path\": \"/api/v2/auth/login\"\n  }\n}")))
    })
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestV2 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received Login V2 request for mobile number: {}", request.mobile());
        LoginResult result = authService.loginV2(request);
        return ResponseEntity.ok(result.payload());
    }

    @PostMapping("/social/google")
    @Operation(summary = "Google ilə sosial giriş V2")
    public ResponseEntity<LoginResponse> socialLoginGoogle(
            @Valid @RequestBody GoogleSocialRequestV2 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received Google V2 social login request");
        LoginResponse response = socialAuthService.socialLoginGoogleV2(request);
        HttpStatus status = response.user().setupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/social/apple")
    @Operation(summary = "Apple ilə sosial giriş V2")
    public ResponseEntity<LoginResponse> socialLoginApple(
            @Valid @RequestBody AppleSocialRequestV2 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received Apple V2 social login request");
        LoginResponse response = socialAuthService.socialLoginAppleV2(request);
        HttpStatus status = response.user().setupRequired() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/registration/register/complete")
    @Operation(summary = "Qeydiyyatı tamamla V2")
    public ResponseEntity<LoginResponse> completeRegistration(
            @Valid @RequestBody RegisterCompleteRequestV2 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received Registration Complete V2 request");
        LoginResponse response = registrationService.completeRegistrationV2(request);
        return ResponseEntity.ok(response);
    }
}
