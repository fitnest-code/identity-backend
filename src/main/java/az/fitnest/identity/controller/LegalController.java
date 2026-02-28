package az.fitnest.identity.controller;
import az.fitnest.identity.model.enums.UserStatus;

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
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
@Tag(name = "Legal & Consents", description = "Endpoints for viewing legal documents and managing user consents")
public class LegalController {

    private final LegalService legalService;

    @GetMapping("/privacy-policy")
    @Operation(summary = "Get Privacy Policy", description = "Retrieves the active privacy policy content.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Privacy policy retrieved successfully", content = @Content(schema = @Schema(implementation = LegalDocumentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Active privacy policy not found")
    })
    public ResponseEntity<LegalDocumentResponse> getPrivacyPolicy(
            @Parameter(description = "Language code (e.g., AZ, EN, RU)") @RequestParam(defaultValue = "AZ") String lang,
            @Parameter(description = "Response format (e.g., html, plain)") @RequestParam(defaultValue = "html") String format) {
        return ResponseEntity.ok(legalService.getPrivacyPolicy(lang, format));
    }

    @GetMapping("/terms-of-use")
    @Operation(summary = "Get Terms of Use", description = "Retrieves the active terms of use content.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Terms of use retrieved successfully", content = @Content(schema = @Schema(implementation = LegalDocumentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Active terms of use not found")
    })
    public ResponseEntity<LegalDocumentResponse> getTermsOfUse(
            @Parameter(description = "Language code (e.g., AZ, EN, RU)") @RequestParam(defaultValue = "AZ") String lang,
            @Parameter(description = "Response format (e.g., html, plain)") @RequestParam(defaultValue = "html") String format) {
        return ResponseEntity.ok(legalService.getTermsOfUse(lang, format));
    }

    @PostMapping("/consents/accept")
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

    @GetMapping("/consents/me")
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

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            throw new az.fitnest.identity.exception.UnauthorizedException("User not authenticated");
        }
        return (Long) authentication.getPrincipal();
    }
}
