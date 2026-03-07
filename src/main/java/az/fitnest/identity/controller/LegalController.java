package az.fitnest.identity.controller;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.LegalDocumentType;
import az.fitnest.identity.dto.*;
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
@Tag(name = "Legal & Consents", description = "Hüquqi sənədlərə baxmaq və istifadəçi razılıqlarını idarə etmək üçün ucluqlar")
public class LegalController {

    private final LegalService legalService;

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

    @PostMapping("/consents/accept")
    @Operation(summary = "Razılıqları qəbul edin", description = "İstifadəçinin cari hüquqi sənədləri qəbul etməsini qeyd edir.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Razılıqlar uğurla qeyd edildi"),
            @ApiResponse(responseCode = "401", description = "İstifadəçi autentifikasiya olunmayıb")
    })
    public ResponseEntity<Void> acceptConsents(
            @Valid @RequestBody ConsentAcceptRequest request,
            HttpServletRequest httpServletRequest) {
        Long userId = getCurrentUserId();
        String ipAddress = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");
        legalService.acceptConsent(userId, request, ipAddress, userAgent);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/consents/me")
    @Operation(summary = "Razılıqlarımı əldə edin", description = "Cari autentifikasiya olunmuş istifadəçi üçün razılıq statusunu yoxlayır.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Razılıq statusu əldə edildi", content = @Content(schema = @Schema(implementation = UserConsentStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "İstifadəçi autentifikasiya olunmayıb")
    })
    public ResponseEntity<UserConsentStatusResponse> getUserConsents() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(legalService.getUserConsentStatus(userId));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            throw new az.fitnest.identity.exception.UnauthorizedException("User not authenticated");
        }
        return (Long) authentication.getPrincipal();
    }
}
