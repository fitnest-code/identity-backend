package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.repository.UserConsentRepository;
import az.fitnest.identity.repository.LegalDocumentRepository;
import az.fitnest.identity.dto.*;
import az.fitnest.identity.model.enums.LegalDocumentType;
import az.fitnest.identity.model.entity.LegalDocument;
import az.fitnest.identity.model.entity.UserConsent;
import az.fitnest.identity.exception.ValidationException;
import az.fitnest.identity.service.LegalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LegalServiceImpl implements LegalService {

    private final UserConsentRepository userConsentRepository;

    private final LegalDocumentRepository legalDocumentRepository;

    @Override
    public LegalDocumentResponse getPrivacyPolicy(String lang, String format) {
        return getDocument(LegalDocumentType.PRIVACY_POLICY, lang);
    }

    @Override
    public LegalDocumentResponse getTermsOfUse(String lang, String format) {
        return getDocument(LegalDocumentType.TERMS_OF_USE, lang);
    }

    private LegalDocumentResponse getDocument(LegalDocumentType type, String lang) {
        String normalizedLang = normalizeLanguage(lang);

        // Fallback to EN if requested lang not found, or just return empty if nothing exists
        LegalDocument doc = legalDocumentRepository.findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(type, normalizedLang)
                .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("Sənəd tapılmadı"));

        return new LegalDocumentResponse(
                type.name().toLowerCase(),
                doc.getVersion(),
                type == LegalDocumentType.PRIVACY_POLICY ? "Məxfilik Siyasəti" : "İstifadə Qaydaları",
                doc.getContent(),
                doc.getPublishedAt()
        );
    }

    @Transactional
    @Override
    public void createDocument(CreateLegalDocumentRequest request) {
        if (legalDocumentRepository.existsByTypeAndVersion(request.type(), request.version())) {
            throw new az.fitnest.identity.exception.ConflictException("Sənəd versiyası artıq mövcuddur");
        }

        String normalizedLang = normalizeLanguage(request.language());

        // Requirement 8: Enforce single active document per type and language
        if (Boolean.TRUE.equals(request.isActive())) {
            var activeDocs = legalDocumentRepository.findAllByTypeAndLanguageAndIsActiveTrue(request.type(), normalizedLang);
            if (!activeDocs.isEmpty()) {
                activeDocs.forEach(d -> d.setActive(false));
                legalDocumentRepository.saveAll(activeDocs);
            }
        }

        LegalDocument doc = LegalDocument.builder()
                .type(request.type())
                .version(request.version())
                .language(normalizedLang)
                .content(request.content())
                .isActive(request.isActive())
                .publishedAt(request.isActive() ? LocalDateTime.now() : null)
                .build();

        legalDocumentRepository.save(doc);
    }

    @Transactional
    @Override
    public void acceptConsent(Long userId, ConsentAcceptRequest request, String ipAddress, String userAgent) {
        // Validation: Ensure versions exist and are ACTIVE
        boolean privacyExistsAndActive = legalDocumentRepository.existsByTypeAndVersionAndIsActiveTrue(LegalDocumentType.PRIVACY_POLICY, request.privacyVersion());
        boolean termsExistsAndActive = legalDocumentRepository.existsByTypeAndVersionAndIsActiveTrue(LegalDocumentType.TERMS_OF_USE, request.termsVersion());

        if (!privacyExistsAndActive || !termsExistsAndActive) {
            throw new ValidationException("Yanlış razılıq versiyası", "INVALID_CONSENT_VERSION");
        }

        // Idempotency check
        Optional<UserConsent> latestConsentOpt = userConsentRepository.findTopByUserIdOrderByAcceptedAtDesc(userId);
        if (latestConsentOpt.isPresent()) {
            UserConsent latest = latestConsentOpt.get();
            if (latest.getPrivacyPolicyVersion().equals(request.privacyVersion()) &&
                    latest.getTermsOfUseVersion().equals(request.termsVersion())) {
                return; // Already accepted these exact versions
            }
        }

        UserConsent consent = UserConsent.builder()
                .userId(userId)
                .privacyPolicyVersion(request.privacyVersion())
                .termsOfUseVersion(request.termsVersion())
                .acceptedAt(LocalDateTime.now())
                .platform(request.platform())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        userConsentRepository.save(consent);
    }

    @Override
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

            return new UserConsentStatusResponse(
                    new UserConsentStatusResponse.ConsentStatus(true, isPrivacyUpToDate),
                    new UserConsentStatusResponse.ConsentStatus(true, isTermsUpToDate),
                    true,
                    isPrivacyUpToDate && isTermsUpToDate,
                    consent.getPrivacyPolicyVersion(),
                    consent.getAcceptedAt()
            );
        }

        return new UserConsentStatusResponse(
                new UserConsentStatusResponse.ConsentStatus(false, false),
                new UserConsentStatusResponse.ConsentStatus(false, false),
                false,
                false,
                null,
                null
        );
    }

    @Override
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

    // ==================== Admin Methods ====================

    @Override
    public List<AdminLegalDocumentResponse> getAllDocuments(LegalDocumentType type, String language, Boolean active) {
        List<LegalDocument> docs;

        if (type != null && language != null) {
            docs = legalDocumentRepository.findAllByTypeAndLanguageOrderByPublishedAtDesc(type, normalizeLanguage(language));
        } else if (type != null && active != null) {
            docs = legalDocumentRepository.findAllByTypeAndIsActiveOrderByPublishedAtDesc(type, active);
        } else if (type != null) {
            docs = legalDocumentRepository.findAllByTypeOrderByPublishedAtDesc(type);
        } else if (active != null) {
            docs = legalDocumentRepository.findAllByIsActiveOrderByPublishedAtDesc(active);
        } else {
            docs = legalDocumentRepository.findAllByOrderByPublishedAtDesc();
        }

        return docs.stream().map(this::toAdminResponse).collect(Collectors.toList());
    }

    public AdminLegalDocumentResponse getDocumentById(Long id) {
        LegalDocument doc = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("Sənəd tapılmadı"));
        return toAdminResponse(doc);
    }

    @Transactional
    @Override
    public AdminLegalDocumentResponse updateDocument(Long id, UpdateLegalDocumentRequest request) {
        LegalDocument doc = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("Sənəd tapılmadı"));

        if (request.version() != null && !request.version().isBlank()) {
            doc.setVersion(request.version());
        }
        if (request.language() != null && !request.language().isBlank()) {
            doc.setLanguage(normalizeLanguage(request.language()));
        }
        if (request.content() != null && !request.content().isBlank()) {
            doc.setContent(request.content());
        }

        legalDocumentRepository.save(doc);
        return toAdminResponse(doc);
    }

    @Transactional
    @Override
    public void deleteDocument(Long id) {
        if (!legalDocumentRepository.existsById(id)) {
            throw new az.fitnest.identity.exception.ResourceNotFoundException("Sənəd tapılmadı");
        }
        legalDocumentRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void activateDocument(Long id) {
        LegalDocument doc = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("Sənəd tapılmadı"));

        // Deactivate all other documents of the same type and language
        var activeDocs = legalDocumentRepository.findAllByTypeAndLanguageAndIsActiveTrue(doc.getType(), doc.getLanguage());
        activeDocs.forEach(d -> d.setActive(false));
        legalDocumentRepository.saveAll(activeDocs);

        doc.setActive(true);
        doc.setPublishedAt(LocalDateTime.now());
        legalDocumentRepository.save(doc);
    }

    @Transactional
    @Override
    public void deactivateDocument(Long id) {
        LegalDocument doc = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("Sənəd tapılmadı"));

        doc.setActive(false);
        legalDocumentRepository.save(doc);
    }

    @Override
    public Page<AdminConsentResponse> getConsents(Long userId, Pageable pageable) {
        Page<UserConsent> consents;
        if (userId != null) {
            consents = userConsentRepository.findAllByUserIdOrderByAcceptedAtDesc(userId, pageable);
        } else {
            consents = userConsentRepository.findAllByOrderByAcceptedAtDesc(pageable);
        }

        return consents.map(c -> new AdminConsentResponse(
                c.getId(),
                c.getUserId(),
                c.getPrivacyPolicyVersion(),
                c.getTermsOfUseVersion(),
                c.getAcceptedAt(),
                c.getIpAddress(),
                c.getUserAgent(),
                c.getPlatform()
        ));
    }

    // ==================== Helper Methods ====================

    private String normalizeLanguage(String lang) {
        if (lang == null || lang.isBlank()) {
            return "EN";
        }
        return lang.trim().toUpperCase();
    }

    private String getLatestVersion(LegalDocumentType type) {
        return legalDocumentRepository.findTopByTypeAndIsActiveTrueOrderByPublishedAtDesc(type)
                .map(LegalDocument::getVersion)
                .orElse(null);
    }

    private AdminLegalDocumentResponse toAdminResponse(LegalDocument doc) {
        return new AdminLegalDocumentResponse(
                doc.getId(),
                doc.getType().name(),
                doc.getVersion(),
                doc.getLanguage(),
                doc.getContent(),
                doc.isActive(),
                doc.getPublishedAt(),
                doc.getCreatedDate(),
                doc.getLastModifiedDate()
        );
    }
}
