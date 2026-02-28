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
@Tag(name = "Legal & Consents Admin", description = "Administrative endpoints for managing legal documents and user consents")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class LegalAdminController {

    private final LegalService legalService;

    @GetMapping("/documents")
    @Operation(summary = "List all documents (Admin)", description = "Lists all versions of legal documents with filtering options. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<AdminLegalDocumentResponse>> getAllDocuments(
            @Parameter(description = "Filter by document type") @RequestParam(required = false) LegalDocumentType type,
            @Parameter(description = "Filter by language") @RequestParam(required = false) String language,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(legalService.getAllDocuments(type, language, active));
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "Get document by ID (Admin)", description = "Retrieves details of a specific legal document version. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document found"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<AdminLegalDocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(legalService.getDocumentById(id));
    }

    @PostMapping("/documents")
    @Operation(summary = "Create document (Admin)", description = "Publishes a new legal document version. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid document data")
    })
    public ResponseEntity<Void> createDocument(@Valid @RequestBody CreateLegalDocumentRequest request) {
        legalService.createDocument(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/documents/{id}")
    @Operation(summary = "Update document (Admin)", description = "Updates content or metadata of an existing legal document version. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document updated successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<AdminLegalDocumentResponse> updateDocument(
            @PathVariable Long id,
            @RequestBody UpdateLegalDocumentRequest request) {
        return ResponseEntity.ok(legalService.updateDocument(id, request));
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "Delete document (Admin)", description = "Deletes a legal document version. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        legalService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/documents/{id}/activate")
    @Operation(summary = "Activate document (Admin)", description = "Sets a document version as active. Only one version per type/language can be active. Requires ADMIN role.")
    public ResponseEntity<Void> activateDocument(@PathVariable Long id) {
        legalService.activateDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/documents/{id}/deactivate")
    @Operation(summary = "Deactivate document (Admin)", description = "Sets a document version as inactive. Requires ADMIN role.")
    public ResponseEntity<Void> deactivateDocument(@PathVariable Long id) {
        legalService.deactivateDocument(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/consents")
    @Operation(summary = "List user consents (Admin)", description = "Lists user consent records with pagination support. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consents retrieved successfully")
    })
    public ResponseEntity<PaginatedResponse<AdminConsentResponse>> getConsents(
            @Parameter(description = "Filter by User ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PaginatedResponse.of(legalService.getConsents(userId, PageRequest.of(page, size))));
    }
}
