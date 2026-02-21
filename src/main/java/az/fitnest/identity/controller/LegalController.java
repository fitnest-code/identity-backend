package az.fitnest.identity.controller;

import az.fitnest.identity.constants.LegalDocumentType;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Legal & Consents", description = "Endpoints for managing legal documents (Privacy Policy, Terms) and user consents")
public class LegalController {

    private final LegalService legalService;

    // --- Public & User Endpoints ---

    @GetMapping("/legal/privacy-policy")
    @Operation(summary = "Get Privacy Policy", description = "Retrieves the active privacy policy content in the specified language and format.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Privacy policy retrieved successfully", content = @Content(schema = @Schema(implementation = LegalDocumentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Active privacy policy not found")
    })
    public ResponseEntity<LegalDocumentResponse> getPrivacyPolicy(
            @Parameter(description = "Language code (e.g., AZ, EN, RU)") @RequestParam(defaultValue = "AZ") String lang,
            @Parameter(description = "Response format (e.g., html, plain)") @RequestParam(defaultValue = "html") String format) {
        return ResponseEntity.ok(legalService.getPrivacyPolicy(lang, format));
    }

    @GetMapping("/legal/terms-of-use")
    @Operation(summary = "Get Terms of Use", description = "Retrieves the active terms of use content in the specified language and format.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Terms of use retrieved successfully", content = @Content(schema = @Schema(implementation = LegalDocumentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Active terms of use not found")
    })
    public ResponseEntity<LegalDocumentResponse> getTermsOfUse(
            @Parameter(description = "Language code (e.g., AZ, EN, RU)") @RequestParam(defaultValue = "AZ") String lang,
            @Parameter(description = "Response format (e.g., html, plain)") @RequestParam(defaultValue = "html") String format) {
        return ResponseEntity.ok(legalService.getTermsOfUse(lang, format));
    }

    @PostMapping("/legal/consents/accept")
    @Operation(summary = "Accept Consents", description = "Records the user's acceptance of current legal documents.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consents recorded successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
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

    @GetMapping("/legal/consents/me")
    @Operation(summary = "Get My Consents", description = "Checks the consent status for the currently authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consent status retrieved", content = @Content(schema = @Schema(implementation = UserConsentStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<UserConsentStatusResponse> getUserConsents() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(legalService.getUserConsentStatus(userId));
    }

    // --- Admin Endpoints ---

    @GetMapping("/admin/legal/documents")
    @Operation(summary = "List all documents (Admin)", description = "Lists all versions of legal documents with filtering options. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
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

    @GetMapping("/admin/legal/documents/{id}")
    @Operation(summary = "Get document by ID (Admin)", description = "Retrieves details of a specific legal document version. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document found"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<AdminLegalDocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(legalService.getDocumentById(id));
    }

    @PostMapping("/admin/legal/documents")
    @Operation(summary = "Create document (Admin)", description = "Publishes a new legal document version. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid document data")
    })
    public ResponseEntity<Void> createDocument(@Valid @RequestBody CreateLegalDocumentRequest request) {
        legalService.createDocument(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/admin/legal/documents/{id}")
    @Operation(summary = "Update document (Admin)", description = "Updates content or metadata of an existing legal document version. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document updated successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<AdminLegalDocumentResponse> updateDocument(
            @PathVariable Long id,
            @RequestBody UpdateLegalDocumentRequest request) {
        return ResponseEntity.ok(legalService.updateDocument(id, request));
    }

    @DeleteMapping("/admin/legal/documents/{id}")
    @Operation(summary = "Delete document (Admin)", description = "Deletes a legal document version. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        legalService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/admin/legal/documents/{id}/activate")
    @Operation(summary = "Activate document (Admin)", description = "Sets a document version as active. Only one version per type/language can be active. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> activateDocument(@PathVariable Long id) {
        legalService.activateDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/admin/legal/documents/{id}/deactivate")
    @Operation(summary = "Deactivate document (Admin)", description = "Sets a document version as inactive. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deactivateDocument(@PathVariable Long id) {
        legalService.deactivateDocument(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/legal/consents")
    @Operation(summary = "List user consents (Admin)", description = "Lists user consent records with pagination support. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consents retrieved successfully")
    })
    public ResponseEntity<Page<AdminConsentResponse>> getConsents(
            @Parameter(description = "Filter by User ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(legalService.getConsents(userId, PageRequest.of(page, size)));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            throw new az.fitnest.identity.exception.UnauthorizedException("User not authenticated");
        }
        return (Long) authentication.getPrincipal();
    }
}
