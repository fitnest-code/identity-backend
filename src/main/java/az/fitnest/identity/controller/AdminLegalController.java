package az.fitnest.identity.controller;

import az.fitnest.identity.service.LegalService;
import az.fitnest.identity.dto.CreateLegalDocumentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/legal/documents")
@RequiredArgsConstructor
@Tag(name = "Admin Legal Management", description = "Endpoints for managing legal documents")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminLegalController {

    private final LegalService legalService;

    @PostMapping
    @Operation(summary = "Create Legal Document", description = "Publish a new version of a legal document.")
    public ResponseEntity<Void> createDocument(@Valid @RequestBody CreateLegalDocumentRequest request) {
        legalService.createDocument(request);
        return ResponseEntity.ok().build();
    }
}
