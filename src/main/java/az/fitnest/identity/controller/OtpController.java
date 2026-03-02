package az.fitnest.identity.controller;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpVerifyRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.dto.OtpVerifyResponse;
import az.fitnest.identity.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/otp")
@RequiredArgsConstructor
@Tag(name = "OTP", description = "OTP kodlarının göndərilməsi və doğrulanması üçün ucluqlar")
public class OtpController {

    private final OtpService otpService;


    @Operation(
            summary = "OTP-ni doğrulayın",
            description = "İstifadəçi tərəfindən təqdim olunan OTP kodunu doğrulayır. " +
                    "Uğurlu doğrulamadan sonra məqsəddən asılı olaraq: " +
                    "- QEYDİYYAT: qeydiyyat tokeni verir. " +
                    "- ŞİFRƏ_SIFIRLAMA: sıfırlama tokeni verir. " +
                    "- REAKTİVASİYA: hesabı yenidən aktivləşdirir və giriş tokenlərini verir. " +
                    "Sessiya bloklanmazdan əvvəl maksimum 5 doğrulama cəhdinə icazə verilir."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP uğurla doğrulandı",
                    content = @Content(schema = @Schema(implementation = OtpVerifyResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Yanlış sorğu məlumatı, OTP sessiyası bloklanıb, OTP artıq doğrulanıb və ya çox sayda cəhd edilib",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Yanlış OTP kodu",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "OTP sessiyası tapılmadı və ya vaxtı keçib",
                    content = @Content
            )
    })
    @PostMapping("/verify")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request
    ) {
        OtpVerifyResponse response = otpService.verifyOtpAndIssueToken(request);
        return ResponseEntity.ok(response);
    }
}