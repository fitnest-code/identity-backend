package az.fitnest.identity.controller;

import az.fitnest.identity.dto.request.*;
import az.fitnest.identity.dto.response.*;
import az.fitnest.identity.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Qeydiyyat", description = "İstifadəçi qeydiyyatı və qeydiyyatın tamamlanması üçün endpointlər.")
@RestController
@RequestMapping("/api/v1/auth/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    @Operation(summary = "Qeydiyyatı başladın", description = "Yeni istifadəçi qeydiyyatını başladır və OTP göndərir.")
    public ResponseEntity<ApiResponse<OtpSendResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        return ResponseEntity.ok(ApiResponse.success(registrationService.startRegistration(request)));
    }

    @PostMapping("/register/complete")
    @Operation(summary = "Qeydiyyatı tamamlayın", description = "OTP və istifadəçi məlumatları ilə qeydiyyatı tamamlayır.")
    public ResponseEntity<LoginResponse> registerComplete(
            @Valid @RequestBody RegisterCompleteRequest request,
            @Parameter(name = "lang", description = "Dil kodu (az, en, ru)", in = ParameterIn.QUERY)
            @RequestParam(required = false) String lang) {
        return ResponseEntity.ok(registrationService.completeRegistration(request));
    }
}
