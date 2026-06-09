package az.fitnest.identity.controller;

import az.fitnest.identity.dto.request.LoginCheckRequestV3;
import az.fitnest.identity.dto.request.LoginRequestV3;
import az.fitnest.identity.dto.request.LoginVerifyRequestV3;
import az.fitnest.identity.dto.request.RegisterCompleteRequestV3;
import az.fitnest.identity.dto.request.RegisterRequestV3;
import az.fitnest.identity.dto.response.ApiResponse;
import az.fitnest.identity.dto.response.LoginEligibilityResponse;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.dto.response.OtpSendResponse;
import az.fitnest.identity.service.AuthService;
import az.fitnest.identity.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autentifikasiya V3", description = "Şifrəsiz OTP əsaslı autentifikasiya V3 endpointləri. Qeydiyyat və giriş yalnız OTP ilə.")
@Slf4j
@RestController
@RequestMapping("/api/v3/auth")
@RequiredArgsConstructor
public class AuthControllerV3 {

    private final AuthService authService;
    private final RegistrationService registrationService;

    @PostMapping("/registration/register")
    @Operation(summary = "Qeydiyyatı başladın V3 (şifrəsiz)", description = "Mobil nömrə ilə qeydiyyatı başladır və OTP göndərir. Şifrə tələb olunmur.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP uğurla göndərildi",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OtpSendResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Bu nömrə artıq qeydiyyatdan keçib",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":{\"code\":\"DUPLICATE_MOBILE\",\"message\":\"Bu nömrə artıq qeydiyyatdan keçib\",\"status\":409}}")))
    })
    public ResponseEntity<ApiResponse<OtpSendResponse>> register(
            @Valid @RequestBody RegisterRequestV3 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received V3 registration request for mobile: {}", request.mobile());
        return ResponseEntity.ok(ApiResponse.success(registrationService.startRegistrationV3(request)));
    }

    @PostMapping("/registration/register/complete")
    @Operation(summary = "Qeydiyyatı tamamlayın V3 (şifrəsiz)", description = "OTP doğrulamasından sonra ad və soyad ilə qeydiyyatı tamamlayır. Şifrə tələb olunmur.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Qeydiyyat uğurla tamamlandı",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Yanlış sorğu formatı")
    })
    public ResponseEntity<LoginResponse> completeRegistration(
            @Valid @RequestBody RegisterCompleteRequestV3 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received V3 registration complete request");
        LoginResponse response = registrationService.completeRegistrationV3(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Girişi başladın V3 (şifrəsiz)", description = "Mobil nömrə ilə giriş OTP-si göndərir. Şifrə tələb olunmur.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP uğurla göndərildi",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OtpSendResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "İstifadəçi tapılmadı və ya hesab bloklanıb",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":{\"code\":\"INVALID_CREDENTIALS\",\"message\":\"Yanlış giriş məlumatları\",\"status\":401}}")))
    })
    public ResponseEntity<ApiResponse<OtpSendResponse>> login(
            @Valid @RequestBody LoginRequestV3 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received V3 login request for mobile: {}", request.mobile());
        OtpSendResponse response = authService.startLoginV3(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login/verify")
    @Operation(summary = "Girişi doğrulayın V3", description = "OTP kodu və cihaz məlumatları ilə girişi doğrulayır. iOS/Android üçün cihaz ID tələb olunur. Cihaz dəyişikliyi limiti 3-dür.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Giriş uğurludur",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "OTP yanlışdır, cihaz limiti keçilib, və ya cihaz ID tələb olunur",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":{\"code\":\"INVALID_CREDENTIALS\",\"message\":\"Limiti keçmisiniz\",\"status\":401}}")))
    })
    public ResponseEntity<LoginResponse> verifyLogin(
            @Valid @RequestBody LoginVerifyRequestV3 request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        log.info("Received V3 login verify request");
        LoginResponse response = authService.verifyLoginV3(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/check")
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
    public ResponseEntity<Void> checkLoginEligibility(
            @Valid @RequestBody LoginCheckRequestV3 request,
            @RequestParam(required = false) String lang) {
        log.info("Received V3 login eligibility check request for mobile: {}", request.mobile());
        authService.checkLoginEligibility(request);
        return ResponseEntity.ok().build();
    }
}
