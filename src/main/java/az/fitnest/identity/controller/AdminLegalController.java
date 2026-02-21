package az.fitnest.identity.controller;

import az.fitnest.identity.constants.LegalDocumentType;
import az.fitnest.identity.dto.AdminConsentResponse;
import az.fitnest.identity.dto.AdminLegalDocumentResponse;
import az.fitnest.identity.dto.CreateLegalDocumentRequest;
import az.fitnest.identity.dto.UpdateLegalDocumentRequest;
import az.fitnest.identity.service.LegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/legal")
@RequiredArgsConstructor
@Tag(name = "Admin Legal Management", description = "Endpoints for managing legal documents and viewing user consents")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminLegalController {

    private final LegalService legalService;

    // ==================== Legal Documents ====================

    @GetMapping("/documents")
    @Operation(summary = "List all legal documents", description = "Returns all legal documents with optional filtering by type, language, and active status.")
    public ResponseEntity<List<AdminLegalDocumentResponse>> getAllDocuments(
            @Parameter(description = "Filter by document type") @RequestParam(required = false) LegalDocumentType type,
            @Parameter(description = "Filter by language code (e.g. AZ, EN)") @RequestParam(required = false) String language,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(legalService.getAllDocuments(type, language, active));
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "Get legal document by ID", description = "Returns a single legal document with all its details.")
    public ResponseEntity<AdminLegalDocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(legalService.getDocumentById(id));
    }

    @PostMapping("/documents")
    @Operation(summary = "Create legal document", description = "Publish a new version of a legal document. If is_active is true, all other active documents of the same type and language are deactivated.")
    public ResponseEntity<Void> createDocument(@Valid @RequestBody CreateLegalDocumentRequest request) {
        legalService.createDocument(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/documents/{id}")
    @Operation(summary = "Update legal document", description = "Update the content, version, or language of an existing legal document. Only provided fields are updated.")
    public ResponseEntity<AdminLegalDocumentResponse> updateDocument(
            @PathVariable Long id,
            @RequestBody UpdateLegalDocumentRequest request) {
        return ResponseEntity.ok(legalService.updateDocument(id, request));
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "Delete legal document", description = "Permanently removes a legal document.")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        legalService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/documents/{id}/activate")
    @Operation(summary = "Activate legal document", description = "Activates the document and deactivates all other documents of the same type and language.")
    public ResponseEntity<Void> activateDocument(@PathVariable Long id) {
        legalService.activateDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/documents/{id}/deactivate")
    @Operation(summary = "Deactivate legal document", description = "Deactivates the document without activating another.")
    public ResponseEntity<Void> deactivateDocument(@PathVariable Long id) {
        legalService.deactivateDocument(id);
        return ResponseEntity.ok().build();
    }

    // ==================== User Consents ====================

    @GetMapping("/consents")
    @Operation(summary = "List user consents", description = "Returns paginated list of user consent records. Optionally filter by user ID.")
    public ResponseEntity<Page<AdminConsentResponse>> getConsents(
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(legalService.getConsents(userId, PageRequest.of(page, size)));
    }
}
