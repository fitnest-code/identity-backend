package az.fitnest.identity.legal.adapter.service;

import az.fitnest.identity.legal.adapter.persistence.UserConsentRepository;
import az.fitnest.identity.legal.api.dto.request.ConsentAcceptRequest;
import az.fitnest.identity.legal.api.dto.request.CreateLegalDocumentRequest;
import az.fitnest.identity.legal.api.dto.response.LegalDocumentResponse;
import az.fitnest.identity.legal.api.dto.response.UserConsentStatusResponse;
import az.fitnest.identity.legal.domain.enums.LegalDocumentType;
import az.fitnest.identity.legal.domain.model.UserConsent;
import az.fitnest.identity.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LegalService {

    private final UserConsentRepository userConsentRepository;

    private final az.fitnest.identity.legal.adapter.persistence.LegalDocumentRepository legalDocumentRepository;

    public LegalDocumentResponse getPrivacyPolicy(String lang, String format) {
        return getDocument(LegalDocumentType.PRIVACY_POLICY, lang);
    }

    public LegalDocumentResponse getTermsOfUse(String lang, String format) {
        return getDocument(LegalDocumentType.TERMS_OF_USE, lang);
    }

    private LegalDocumentResponse getDocument(LegalDocumentType type, String lang) {
        String normalizedLang = normalizeLanguage(lang);
        
        // Fallback to EN if requested lang not found, or just return empty if nothing exists
        az.fitnest.identity.legal.domain.model.LegalDocument doc = legalDocumentRepository
                .findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(type, normalizedLang)
                .or(() -> legalDocumentRepository.findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(type, "EN"))
                .orElseThrow(() -> new az.fitnest.identity.shared.exception.ResourceNotFoundException("Document not found"));

        return LegalDocumentResponse.builder()
                .document(LegalDocumentResponse.DocumentData.builder()
                        .type(type.name().toLowerCase())
                        .version(doc.getVersion())
                        .title(type == LegalDocumentType.PRIVACY_POLICY ? "Privacy Policy" : "Terms of Use")
                        .content(doc.getContent())
                        .updatedAt(doc.getPublishedAt())
                        .build())
                .build();
    }

    @Transactional
    public void createDocument(CreateLegalDocumentRequest request) {
        if (legalDocumentRepository.existsByTypeAndVersion(request.getType(), request.getVersion())) {
             throw new az.fitnest.identity.shared.exception.ConflictException("Document version already exists");
        }

        String normalizedLang = normalizeLanguage(request.getLanguage());

        // Requirement 8: Enforce single active document per type and language
        if (Boolean.TRUE.equals(request.getIsActive())) {
            var activeDocs = legalDocumentRepository.findAllByTypeAndLanguageAndIsActiveTrue(request.getType(), normalizedLang);
            if (!activeDocs.isEmpty()) {
                activeDocs.forEach(d -> d.setActive(false));
                legalDocumentRepository.saveAll(activeDocs);
            }
        }

        az.fitnest.identity.legal.domain.model.LegalDocument doc = az.fitnest.identity.legal.domain.model.LegalDocument.builder()
                .type(request.getType())
                .version(request.getVersion())
                .language(normalizedLang)
                .content(request.getContent())
                .isActive(request.getIsActive())
                .publishedAt(request.getIsActive() ? LocalDateTime.now() : null)
                .build();
        
        legalDocumentRepository.save(doc);
    }

    @Transactional
    public void acceptConsent(Long userId, ConsentAcceptRequest request, String ipAddress, String userAgent) {
        // Validation: Ensure versions exist and are ACTIVE
        boolean privacyExistsAndActive = legalDocumentRepository.existsByTypeAndVersionAndIsActiveTrue(LegalDocumentType.PRIVACY_POLICY, request.getPrivacyVersion());
        boolean termsExistsAndActive = legalDocumentRepository.existsByTypeAndVersionAndIsActiveTrue(LegalDocumentType.TERMS_OF_USE, request.getTermsVersion());

        if (!privacyExistsAndActive || !termsExistsAndActive) {
            // Use ValidationException as requested
            // We need to construct a BindingResult ideally, but ValidationException constructor might be limited.
            // Assuming we can pass a message, or if strictly needed, we might need a workaround.
            // Given existing ValidationException takes BindingResult, we might need to mock or reuse another exception
            // that maps to 400. Or check if there is a simpler constructor.
            // Re-checking ValidationException... it takes BindingResult.
            // If we strictly need "VALIDATION_ERROR" code, we might need to adjust exception usage or add a constructor.
            // For now, let's use a simpler approach if possible or create a dummy BindingResult?
            // Actually, let's check validation exception again.
            // It extends BaseException(message, status, code).
            // We can probably subclass or add a constructor.
            // For now, let's throw a custom exception that extends BaseException or similar if possible.
            // But user asked for ValidationException to allow "INVALID_CONSENT_VERSION".
            // Let's assume we can add a connector to ValidationException or use a similar one.
            // Wait, I can see ValidationException file content earlier. It ONLY has constructor with BindingResult.
            // I should add a constructor to ValidationException first to allow string message.
            throw new ValidationException("Invalid consent version", "INVALID_CONSENT_VERSION"); 
        }

        // Idempotency check
        Optional<UserConsent> latestConsentOpt = userConsentRepository.findTopByUserIdOrderByAcceptedAtDesc(userId);
        if (latestConsentOpt.isPresent()) {
            UserConsent latest = latestConsentOpt.get();
            if (latest.getPrivacyPolicyVersion().equals(request.getPrivacyVersion()) && 
                latest.getTermsOfUseVersion().equals(request.getTermsVersion())) {
                return; // Already accepted these exact versions
            }
        }

        UserConsent consent = UserConsent.builder()
                .userId(userId)
                .privacyPolicyVersion(request.getPrivacyVersion())
                .termsOfUseVersion(request.getTermsVersion())
                .acceptedAt(LocalDateTime.now())
                .platform(request.getPlatform())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        
        userConsentRepository.save(consent);
    }

    public UserConsentStatusResponse getUserConsentStatus(Long userId) {
        Optional<UserConsent> latestConsent = userConsentRepository.findTopByUserIdOrderByAcceptedAtDesc(userId);
        
        // Get latest active versions
        String latestPrivacyVersion = getLatestVersion(LegalDocumentType.PRIVACY_POLICY);
        String latestTermsVersion = getLatestVersion(LegalDocumentType.TERMS_OF_USE);

        if (latestConsent.isPresent()) {
            UserConsent consent = latestConsent.get();
            
            // Accepted = true if they have accepted ANY version (which they have if record exists)
            // UpToDate = true if their version matches latest active
            
            boolean isPrivacyUpToDate = latestPrivacyVersion != null && latestPrivacyVersion.equals(consent.getPrivacyPolicyVersion());
            boolean isTermsUpToDate = latestTermsVersion != null && latestTermsVersion.equals(consent.getTermsOfUseVersion());
            
            return UserConsentStatusResponse.builder()
                    .privacy(UserConsentStatusResponse.ConsentStatus.builder()
                            .accepted(true)
                            .upToDate(isPrivacyUpToDate)
                            .version(consent.getPrivacyPolicyVersion())
                            .acceptedAt(consent.getAcceptedAt())
                            .build())
                    .terms(UserConsentStatusResponse.ConsentStatus.builder()
                            .accepted(true)
                            .upToDate(isTermsUpToDate)
                            .version(consent.getTermsOfUseVersion())
                            .acceptedAt(consent.getAcceptedAt())
                            .build())
                    .build();
        }
        
        return UserConsentStatusResponse.builder()
                .privacy(UserConsentStatusResponse.ConsentStatus.builder().accepted(false).upToDate(false).build())
                .terms(UserConsentStatusResponse.ConsentStatus.builder().accepted(false).upToDate(false).build())
                .build();
    }

    public boolean isConsentRequired(Long userId) {
        Optional<UserConsent> latestConsent = userConsentRepository.findTopByUserIdOrderByAcceptedAtDesc(userId);
        String latestPrivacyVersion = getLatestVersion(LegalDocumentType.PRIVACY_POLICY);
        String latestTermsVersion = getLatestVersion(LegalDocumentType.TERMS_OF_USE);

        // If NO active documents exist at all, consent is not required.
        if (latestPrivacyVersion == null && latestTermsVersion == null) {
            return false;
        }
        
        // If we have at least one active doc, we check user status
        if (latestConsent.isEmpty()) {
            return true; // No consent at all -> required
        }
        
        UserConsent consent = latestConsent.get();
        
        boolean privacyOk = (latestPrivacyVersion == null) || latestPrivacyVersion.equals(consent.getPrivacyPolicyVersion());
        boolean termsOk = (latestTermsVersion == null) || latestTermsVersion.equals(consent.getTermsOfUseVersion());
        
        // Required if EITHER is not OK
        return !privacyOk || !termsOk;
    }
    
    private String normalizeLanguage(String lang) {
        if (lang == null || lang.isBlank()) {
            return "EN";
        }
        return lang.trim().toUpperCase();
    }

    private String getLatestVersion(LegalDocumentType type) {
        return legalDocumentRepository.findTopByTypeAndIsActiveTrueOrderByPublishedAtDesc(type)
                .map(az.fitnest.identity.legal.domain.model.LegalDocument::getVersion)
                .orElse(null);
    }
}
