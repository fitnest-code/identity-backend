package az.fitnest.identity.controller;

import az.fitnest.identity.dto.request.*;
import az.fitnest.identity.dto.response.*;
import az.fitnest.identity.model.enums.LegalDocumentType;
import az.fitnest.identity.service.LegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/legal")
@RequiredArgsConstructor
@Tag(name = "Hüquqi İdarəetmə Admin", description = "Hüquqi sənədlərin (Məxfilik Siyasəti, İstifadə Şərtləri) və istifadəçi razılıqlarının idarə olunması üçün admin endpointləri.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class LegalAdminController {

    private final LegalService legalService;

    @Operation(summary = "Yeni hüquqi sənəd yaradın", description = "Yeni məxfilik siyasəti və ya istifadə şərtləri versiyası yaradılır.")
    @PostMapping("/documents")
    public ResponseEntity<Void> createDocument(@Valid @RequestBody CreateLegalDocumentRequest request) {
        legalService.createDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Sənədləri siyahılayın", description = "Sistemdəki bütün hüquqi sənədləri filtr əsasında qaytarır.")
    @GetMapping("/documents")
    public ResponseEntity<List<AdminLegalDocumentResponse>> getAllDocuments(
            @RequestParam(required = false) LegalDocumentType type,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(legalService.getAllDocuments(type, language, active));
    }

    @Operation(summary = "Sənədi ID-si ilə əldə edin", description = "Sənədi ID-si ilə əldə edir.")
    @GetMapping("/documents/{id}")
    public ResponseEntity<AdminLegalDocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(legalService.getDocumentById(id));
    }

    @Operation(summary = "Sənədi yeniləyin", description = "Sənədi yeniləyir.")
    @PutMapping("/documents/{id}")
    public ResponseEntity<AdminLegalDocumentResponse> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLegalDocumentRequest request) {
        return ResponseEntity.ok(legalService.updateDocument(id, request));
    }

    @Operation(summary = "Sənədi silin", description = "Sənədi silir.")
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        legalService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Sənədi aktivləşdirin", description = "Seçilmiş sənəd versiyasını aktiv edir və eyni tipli köhnə aktiv sənədləri deaktiv edir.")
    @PostMapping("/documents/{id}/activate")
    public ResponseEntity<Void> activateDocument(@PathVariable Long id) {
        legalService.activateDocument(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Sənədi deaktivləşdirin", description = "Sənədi deaktiv edir.")
    @PostMapping("/documents/{id}/deactivate")
    public ResponseEntity<Void> deactivateDocument(@PathVariable Long id) {
        legalService.deactivateDocument(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İstifadəçi razılıqlarını izləyin", description = "İstifadəçilər tərəfindən qəbul edilmiş razılıqların siyahısını qaytarır.")
    @GetMapping("/consents")
    public ResponseEntity<Page<AdminConsentResponse>> getConsents(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(legalService.getConsents(userId, PageRequest.of(page, size)));
    }
}
