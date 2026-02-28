package az.fitnest.identity.controller;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.LegalDocumentType;
import az.fitnest.identity.dto.*;
import az.fitnest.identity.service.LegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/legal")
@RequiredArgsConstructor
@Tag(name = "Legal & Consents Admin", description = "Hüquqi sənədləri və istifadəçi razılıqlarını idarə etmək üçün administrativ ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class LegalAdminController {

    private final LegalService legalService;

    @GetMapping("/documents")
    @Operation(summary = "Bütün sənədlərin siyahısı (Admin)", description = "Filtrləmə seçimləri ilə bütün hüquqi sənəd versiyalarını sadalayır. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sənədlər uğurla əldə edildi"),
            @ApiResponse(responseCode = "403", description = "Kifayət qədər icazə yoxdur")
    })
    public ResponseEntity<List<AdminLegalDocumentResponse>> getAllDocuments(
            @Parameter(description = "Sənəd növünə görə filtrləyin") @RequestParam(required = false) LegalDocumentType type,
            @Parameter(description = "Dilə görə filtrləyin") @RequestParam(required = false) String language,
            @Parameter(description = "Aktiv statusa görə filtrləyin") @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(legalService.getAllDocuments(type, language, active));
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "Sənədi ID vasitəsilə əldə edin (Admin)", description = "Xüsusi hüquqi sənəd versiyasının təfərrüatlarını əldə edir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sənəd tapıldı"),
            @ApiResponse(responseCode = "404", description = "Sənəd tapılmadı")
    })
    public ResponseEntity<AdminLegalDocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(legalService.getDocumentById(id));
    }

    @PostMapping("/documents")
    @Operation(summary = "Sənəd yaradın (Admin)", description = "Yeni hüquqi sənəd versiyasını dərc edir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sənəd uğurla yaradıldı"),
            @ApiResponse(responseCode = "400", description = "Yanlış sənəd məlumatı")
    })
    public ResponseEntity<Void> createDocument(@Valid @RequestBody CreateLegalDocumentRequest request) {
        legalService.createDocument(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/documents/{id}")
    @Operation(summary = "Sənədi yeniləyin (Admin)", description = "Mövcud hüquqi sənəd versiyasının məzmununu və ya metasını yeniləyir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sənəd uğurla yeniləndi"),
            @ApiResponse(responseCode = "404", description = "Sənəd tapılmadı")
    })
    public ResponseEntity<AdminLegalDocumentResponse> updateDocument(
            @PathVariable Long id,
            @RequestBody UpdateLegalDocumentRequest request) {
        return ResponseEntity.ok(legalService.updateDocument(id, request));
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "Sənədi silin (Admin)", description = "Hüquqi sənəd versiyasını silir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sənəd uğurla silindi"),
            @ApiResponse(responseCode = "404", description = "Sənəd tapılmadı")
    })
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        legalService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/documents/{id}/activate")
    @Operation(summary = "Sənədi aktivləşdirin (Admin)", description = "Sənəd versiyasını aktivləşdirir. Hər növ/dil üçün yalnız bir versiya aktiv ola bilər. ADMIN rolu tələb olunur.")
    public ResponseEntity<Void> activateDocument(@PathVariable Long id) {
        legalService.activateDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/documents/{id}/deactivate")
    @Operation(summary = "Sənədi deaktiv edin (Admin)", description = "Sənəd versiyasını deaktiv edir. ADMIN rolu tələb olunur.")
    public ResponseEntity<Void> deactivateDocument(@PathVariable Long id) {
        legalService.deactivateDocument(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/consents")
    @Operation(summary = "İstifadəçi razılıqlarının siyahısı (Admin)", description = "Səhifələmə dəstəyi ilə istifadəçi razılıq yazılarını sadalayır. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Razılıqlar uğurla əldə edildi")
    })
    public ResponseEntity<PaginatedResponse<AdminConsentResponse>> getConsents(
            @Parameter(description = "İstifadəçi ID-sinə görə filtrləyin") @RequestParam(required = false) Long userId,
            @Parameter(description = "Səhifə indeksi (0-dan başlayaraq)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Səhifə ölçüsü") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PaginatedResponse.of(legalService.getConsents(userId, PageRequest.of(page, size))));
    }
}
