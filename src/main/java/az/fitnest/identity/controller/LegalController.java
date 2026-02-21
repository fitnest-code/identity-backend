package az.fitnest.identity.controller;

import az.fitnest.identity.service.LegalService;
import az.fitnest.identity.dto.ConsentAcceptRequest;
import az.fitnest.identity.dto.LegalDocumentResponse;
import az.fitnest.identity.dto.UserConsentStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
@Tag(name = "Legal & Consents", description = "Endpoints for legal documents and consent management")
public class LegalController {

    private final LegalService legalService;

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

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            throw new az.fitnest.identity.exception.UnauthorizedException("User not authenticated");
        }
        return (Long) authentication.getPrincipal();
    }
}
