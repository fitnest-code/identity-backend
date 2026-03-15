package az.fitnest.identity.controller;

import az.fitnest.identity.util.UserContext;
import az.fitnest.identity.model.enums.UserStatus;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import az.fitnest.identity.model.enums.LegalDocumentType;
import az.fitnest.identity.dto.request.*;
import az.fitnest.identity.dto.response.*;
import az.fitnest.identity.service.LegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
@Tag(name = "Hüquqi & Razılıqlar", description = "Hüquqi sənədlərə baxmaq və istifadəçi razılıqlarını idarə etmək üçün endpointlər.")
public class LegalController {

    private final LegalService legalService;
    private final MessageSource messageSource;

    @GetMapping("/privacy-policy")
    @Operation(summary = "Məxfilik Siyasətini əldə edin", description = "Aktiv məxfilik siyasəti məzmununu əldə edir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Məxfilik siyasəti uğurla əldə edildi"),
            @ApiResponse(responseCode = "404", description = "Aktiv məxfilik siyasəti tapılmadı")
    })
    public ResponseEntity<LegalDocumentResponse> getPrivacyPolicy(
            @Parameter(description = "Dil kodu (məsələn, AZ, EN, RU)") @RequestParam(defaultValue = "AZ") String lang,
            @Parameter(description = "Cavab formatı (məsələn, html, plain)") @RequestParam(defaultValue = "html") String format) {
        return ResponseEntity.ok(legalService.getPrivacyPolicy(lang, format));
    }

    @GetMapping("/terms-of-use")
    @Operation(summary = "İstifadə Şərtlərini əldə edin", description = "Aktiv istifadə şərtləri məzmununu əldə edir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "İstifadə şərtləri uğurla əldə edildi", content = @Content(schema = @Schema(implementation = LegalDocumentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Aktiv istifadə şərtləri tapılmadı")
    })
    public ResponseEntity<LegalDocumentResponse> getTermsOfUse(
            @Parameter(description = "Dil kodu (məsələn, AZ, EN, RU)") @RequestParam(defaultValue = "AZ") String lang,
            @Parameter(description = "Cavab formatı (məsələn, html, plain)") @RequestParam(defaultValue = "html") String format) {
        return ResponseEntity.ok(legalService.getTermsOfUse(lang, format));
    }

    @GetMapping("/consents/me")
    @Operation(summary = "Razılıqlarımı əldə edin", description = "Cari autentifikasiya olunmuş istifadəçi üçün razılıq statusunu yoxlayır.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Razılıq statusu əldə edildi", content = @Content(schema = @Schema(implementation = UserConsentStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "İstifadəçi autentifikasiya olunmayıb")
    })
    public ResponseEntity<UserConsentStatusResponse> getUserConsents() {
        Long userId = UserContext.getRequiredUserId();
        return ResponseEntity.ok(legalService.getUserConsentStatus(userId));
    }

}
