package az.fitnest.iam.legal.api;

import az.fitnest.iam.legal.adapter.service.LegalService;
import az.fitnest.iam.legal.api.dto.request.ConsentAcceptRequest;
import az.fitnest.iam.legal.api.dto.response.LegalDocumentResponse;
import az.fitnest.iam.legal.api.dto.response.UserConsentStatusResponse;
import az.fitnest.iam.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Legal & Consents", description = "Endpoints for legal documents and consent management")
public class LegalController {

    private final LegalService legalService;

    @GetMapping("/legal/privacy-policy")
    @Operation(summary = "Get Privacy Policy", description = "Returns the content and version of the privacy policy.")
    public ResponseEntity<LegalDocumentResponse> getPrivacyPolicy(
            @RequestParam(defaultValue = "AZ") String lang,
            @RequestParam(defaultValue = "html") String format) {
        return ResponseEntity.ok(legalService.getPrivacyPolicy(lang, format));
    }

    @GetMapping("/legal/terms-of-use")
    @Operation(summary = "Get Terms of Use", description = "Returns the content and version of the terms of use.")
    public ResponseEntity<LegalDocumentResponse> getTermsOfUse(
            @RequestParam(defaultValue = "AZ") String lang,
            @RequestParam(defaultValue = "html") String format) {
        return ResponseEntity.ok(legalService.getTermsOfUse(lang, format));
    }

    @PostMapping("/legal/consents/accept")
    @Operation(summary = "Accept Consents", description = "Records user acceptance of privacy policy and terms of use.")
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

    @GetMapping("/me/consents")
    @Operation(summary = "Get User Consents", description = "Returns the current user's consent status.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserConsentStatusResponse> getUserConsents() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(legalService.getUserConsentStatus(userId));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            // Should be handled by security filter, but safe guard
            throw new az.fitnest.iam.shared.exception.UnauthorizedException("User not authenticated");
        }
        return (Long) authentication.getPrincipal();
    }
}
