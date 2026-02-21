package az.fitnest.identity.controller;

import az.fitnest.identity.constants.LegalDocumentType;
import az.fitnest.identity.dto.*;
import az.fitnest.identity.service.LegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
@Tag(name = "Legal & Consents", description = "Endpoints for legal documents and consent management")
public class LegalController {

    private final LegalService legalService;

    // ==================== Public Endpoints ====================

    @GetMapping("/privacy-policy")
    @Operation(summary = "Get Privacy Policy", description = "Returns the content and version of the privacy policy.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Privacy Policy retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LegalDocumentResponse.class)))
    })
    public ResponseEntity<LegalDocumentResponse> getPrivacyPolicy(
            @RequestParam(defaultValue = "AZ") String lang,
            @RequestParam(defaultValue = "html") String format) {
        return ResponseEntity.ok(legalService.getPrivacyPolicy(lang, format));
    }

    @GetMapping("/terms-of-use")
    @Operation(summary = "Get Terms of Use", description = "Returns the content and version of the terms of use.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Terms of Use retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LegalDocumentResponse.class)))
    })
    public ResponseEntity<LegalDocumentResponse> getTermsOfUse(
            @RequestParam(defaultValue = "AZ") String lang,
            @RequestParam(defaultValue = "html") String format) {
        return ResponseEntity.ok(legalService.getTermsOfUse(lang, format));
    }

    // ==================== Authenticated User Endpoints ====================

    @PostMapping("/consents/accept")
    @Operation(summary = "Accept Consents", description = "Records user acceptance of privacy policy and terms of use.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consents accepted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
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
    @Operation(summary = "Get User Consents", description = "Returns the current user's consent status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User consent status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserConsentStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserConsentStatusResponse> getUserConsents() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(legalService.getUserConsentStatus(userId));
    }

    // ==================== Admin Endpoints ====================

    @GetMapping("/admin/documents")
    @Operation(summary = "List all legal documents", description = "Returns all legal documents with optional filtering. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<AdminLegalDocumentResponse>> getAllDocuments(
            @Parameter(description = "Filter by document type") @RequestParam(required = false) LegalDocumentType type,
            @Parameter(description = "Filter by language code (e.g. AZ, EN)") @RequestParam(required = false) String language,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(legalService.getAllDocuments(type, language, active));
    }

    @GetMapping("/admin/documents/{id}")
    @Operation(summary = "Get legal document by ID", description = "Returns a single legal document with all its details. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdminLegalDocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(legalService.getDocumentById(id));
    }

    @PostMapping("/admin/documents")
    @Operation(summary = "Create legal document", description = "Publish a new version of a legal document. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> createDocument(@Valid @RequestBody CreateLegalDocumentRequest request) {
        legalService.createDocument(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/admin/documents/{id}")
    @Operation(summary = "Update legal document", description = "Update content, version, or language. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdminLegalDocumentResponse> updateDocument(
            @PathVariable Long id,
            @RequestBody UpdateLegalDocumentRequest request) {
        return ResponseEntity.ok(legalService.updateDocument(id, request));
    }

    @DeleteMapping("/admin/documents/{id}")
    @Operation(summary = "Delete legal document", description = "Permanently removes a legal document. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        legalService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/admin/documents/{id}/activate")
    @Operation(summary = "Activate legal document", description = "Activates the document and deactivates others of same type+language. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> activateDocument(@PathVariable Long id) {
        legalService.activateDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/admin/documents/{id}/deactivate")
    @Operation(summary = "Deactivate legal document", description = "Deactivates the document. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deactivateDocument(@PathVariable Long id) {
        legalService.deactivateDocument(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/consents")
    @Operation(summary = "List user consents", description = "Returns paginated list of user consent records. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Page<AdminConsentResponse>> getConsents(
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(legalService.getConsents(userId, PageRequest.of(page, size)));
    }

    // ==================== Helper ====================

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            throw new az.fitnest.identity.exception.UnauthorizedException("User not authenticated");
        }
        return (Long) authentication.getPrincipal();
    }
}
