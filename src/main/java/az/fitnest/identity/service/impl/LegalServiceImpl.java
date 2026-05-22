package az.fitnest.identity.service.impl;

import az.fitnest.identity.repository.UserConsentRepository;
import az.fitnest.identity.repository.LegalDocumentRepository;
import az.fitnest.identity.dto.request.*;
import az.fitnest.identity.dto.response.*;
import az.fitnest.identity.model.enums.LegalDocumentType;
import az.fitnest.identity.model.entity.LegalDocument;
import az.fitnest.identity.model.entity.UserConsent;
import az.fitnest.identity.exception.ValidationException;
import az.fitnest.identity.service.LegalService;
import az.fitnest.identity.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import az.fitnest.identity.mapper.AdminConsentResponseMapper;
import az.fitnest.identity.mapper.LegalDocumentResponseMapper;
import az.fitnest.identity.mapper.UserConsentStatusResponseMapper;

@Service
@RequiredArgsConstructor
public class LegalServiceImpl implements LegalService {

    private final UserConsentRepository userConsentRepository;

    private final LegalDocumentRepository legalDocumentRepository;

    private final LegalDocumentResponseMapper legalDocumentResponseMapper;
    private final UserConsentStatusResponseMapper userConsentStatusResponseMapper;
    private final AdminConsentResponseMapper adminConsentResponseMapper;
    private final TranslationService translationService;

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

        Optional<LegalDocument> docOpt = legalDocumentRepository.findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(type, "AZ");
        LegalDocument doc;
        if (docOpt.isPresent()) {
            doc = docOpt.get();
        } else {
            doc = legalDocumentRepository.findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(type, normalizedLang)
                    .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("Sənəd tapılmadı"));
        }

        String content = doc.getContent();
        if (!"AZ".equalsIgnoreCase(normalizedLang)) {
            String translated = translationService.getTranslatedValue("LEGAL_DOCUMENT", doc.getId().toString(), "content", normalizedLang);
            if (translated != null && !translated.isBlank()) {
                content = translated;
            }
        }

        return new LegalDocumentResponse(
                doc.getVersion(),
                type.name(),
                content,
                doc.getLastModifiedDate()
        );
    }

    @Transactional
    @Override
    public void createDocument(CreateLegalDocumentRequest request) {
        String normalizedLang = normalizeLanguage(request.language());

        if (legalDocumentRepository.existsByTypeAndLanguageAndVersion(request.type(), normalizedLang, request.version())) {
            throw new ValidationException("error.legal.version_exists", "LEGAL_VERSION_EXISTS");
        }

        ensureVersionIsLatest(request.type(), normalizedLang, request.version());

        LegalDocument doc = LegalDocument.builder()
                .type(request.type())
                .version(request.version())
                .language(normalizedLang)
                .content(request.content())
                .isActive(false)
                .publishedAt(null)
                .build();

        legalDocumentRepository.save(doc);

        if ("AZ".equalsIgnoreCase(normalizedLang)) {
            translationService.autoTranslateAndSave("LEGAL_DOCUMENT", doc.getId().toString(), "content", doc.getContent());
        }
    }

    @Transactional
    @Override
    public void acceptConsent(Long userId, ConsentAcceptRequest request, String ipAddress, String userAgent) {
        boolean privacyExistsAndActive = legalDocumentRepository.existsByTypeAndVersionAndIsActiveTrue(LegalDocumentType.PRIVACY_POLICY, request.privacyVersion());
        boolean termsExistsAndActive = legalDocumentRepository.existsByTypeAndVersionAndIsActiveTrue(LegalDocumentType.TERMS_OF_USE, request.termsVersion());

        if (!privacyExistsAndActive || !termsExistsAndActive) {
            throw new ValidationException("Yanlış razılıq versiyası", "INVALID_CONSENT_VERSION");
        }

        Optional<UserConsent> latestConsentOpt = userConsentRepository.findTopByUserIdOrderByAcceptedAtDesc(userId);
        if (latestConsentOpt.isPresent()) {
            UserConsent latest = latestConsentOpt.get();
            if (latest.getPrivacyPolicyVersion().equals(request.privacyVersion()) &&
                    latest.getTermsOfUseVersion().equals(request.termsVersion())) {
                return;
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

    @Transactional
    @Override
    public void acceptLatestConsents(Long userId, String platform, String ipAddress, String userAgent) {
        String latestPrivacyVersion = getLatestVersion(LegalDocumentType.PRIVACY_POLICY);
        String latestTermsVersion = getLatestVersion(LegalDocumentType.TERMS_OF_USE);

        if (latestPrivacyVersion == null || latestTermsVersion == null) {
            throw new ValidationException("Heç bir aktiv hüquqi sənəd tapılmadı", "MISSING_LEGAL_DOCUMENTS");
        }

        UserConsent consent = UserConsent.builder()
                .userId(userId)
                .privacyPolicyVersion(latestPrivacyVersion)
                .termsOfUseVersion(latestTermsVersion)
                .acceptedAt(LocalDateTime.now())
                .platform(platform != null ? platform : "UNKNOWN")
                .ipAddress(ipAddress != null ? ipAddress : "0.0.0.0")
                .userAgent(userAgent != null ? userAgent : "UNKNOWN")
                .build();

        userConsentRepository.save(consent);
    }

    @Transactional
    @Override
    public void autoAcceptLatestConsents(Long userId) {
        acceptLatestConsents(userId, "SYSTEM", "0.0.0.0", "SYSTEM-AUTO-ACCEPT");
    }

    @Override
    public UserConsentStatusResponse getUserConsentStatus(Long userId) {
        Optional<UserConsent> latestConsent = userConsentRepository.findTopByUserIdOrderByAcceptedAtDesc(userId);

        String latestPrivacyVersion = getLatestVersion(LegalDocumentType.PRIVACY_POLICY);
        String latestTermsVersion = getLatestVersion(LegalDocumentType.TERMS_OF_USE);

        if (latestConsent.isPresent()) {
            UserConsent consent = latestConsent.get();
            return userConsentStatusResponseMapper.toResponse(consent, latestPrivacyVersion, latestTermsVersion);
        }

        return new UserConsentStatusResponse(
                new UserConsentStatusResponse.ConsentDetail(false, false, null, latestPrivacyVersion, null),
                new UserConsentStatusResponse.ConsentDetail(false, false, null, latestTermsVersion, null),
                false
        );
    }

    @Override
    public boolean isConsentRequired(Long userId) {
        Optional<UserConsent> latestConsent = userConsentRepository.findTopByUserIdOrderByAcceptedAtDesc(userId);
        String latestPrivacyVersion = getLatestVersion(LegalDocumentType.PRIVACY_POLICY);
        String latestTermsVersion = getLatestVersion(LegalDocumentType.TERMS_OF_USE);

        if (latestPrivacyVersion == null && latestTermsVersion == null) {
            return false;
        }

        if (latestConsent.isEmpty()) {
            return true;
        }

        UserConsent consent = latestConsent.get();

        boolean privacyOk = (latestPrivacyVersion == null) || latestPrivacyVersion.equals(consent.getPrivacyPolicyVersion());
        boolean termsOk = (latestTermsVersion == null) || latestTermsVersion.equals(consent.getTermsOfUseVersion());

        return !privacyOk || !termsOk;
    }

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

        String targetLanguage = doc.getLanguage();
        String targetVersion = doc.getVersion();

        if (request.language() != null && !request.language().isBlank()) {
            targetLanguage = normalizeLanguage(request.language());
        }
        if (request.version() != null && !request.version().isBlank()) {
            targetVersion = request.version().trim();
        }

        boolean hasVersionChange = !targetVersion.equals(doc.getVersion()) || !targetLanguage.equals(doc.getLanguage());
        if (hasVersionChange) {
            parseVersionParts(targetVersion);
            if (legalDocumentRepository.existsByTypeAndLanguageAndVersionAndIdNot(doc.getType(), targetLanguage, targetVersion, doc.getId())) {
                throw new ValidationException("error.legal.version_exists", "LEGAL_VERSION_EXISTS");
            }
            ensureVersionIsLatestExcludingId(doc.getType(), targetLanguage, targetVersion, doc.getId());
        }

        if (request.version() != null && !request.version().isBlank()) {
            doc.setVersion(targetVersion);
        }
        if (request.language() != null && !request.language().isBlank()) {
            doc.setLanguage(targetLanguage);
        }
        if (request.content() != null && !request.content().isBlank()) {
            doc.setContent(request.content());
        }

        legalDocumentRepository.save(doc);

        if ("AZ".equalsIgnoreCase(doc.getLanguage())) {
            translationService.autoTranslateAndSave("LEGAL_DOCUMENT", doc.getId().toString(), "content", doc.getContent());
        }

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

        var activeDocs = legalDocumentRepository.findAllByTypeAndLanguageAndIsActiveTrue(doc.getType(), doc.getLanguage());
        boolean hasOtherActive = activeDocs.stream().anyMatch(active -> !active.getId().equals(doc.getId()));
        if (hasOtherActive) {
            throw new ValidationException("error.legal.active_document_exists", "LEGAL_ACTIVE_EXISTS");
        }

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

        return consents.map(c -> adminConsentResponseMapper.toResponse(c));
    }

    private String normalizeLanguage(String lang) {
        if (lang == null || lang.isBlank()) {
            return "EN";
        }
        return lang.trim().toUpperCase();
    }

    private String getLatestVersion(LegalDocumentType type) {
        return legalDocumentRepository.findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(type, "AZ")
                .map(LegalDocument::getVersion)
                .orElseGet(() -> legalDocumentRepository.findTopByTypeAndIsActiveTrueOrderByPublishedAtDesc(type)
                        .map(LegalDocument::getVersion)
                        .orElse(null));
    }

    private void ensureVersionIsLatest(LegalDocumentType type, String language, String newVersion) {
        List<LegalDocument> docs = legalDocumentRepository.findAllByTypeAndLanguageOrderByPublishedAtDesc(type, language);
        String latest = null;

        for (LegalDocument doc : docs) {
            String candidate = doc.getVersion();
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (latest == null || compareVersions(candidate, latest) > 0) {
                latest = candidate;
            }
        }

        if (latest != null && compareVersions(newVersion, latest) <= 0) {
            throw new ValidationException("error.legal.version_not_latest", "LEGAL_VERSION_NOT_LATEST");
        }
    }

    private void ensureVersionIsLatestExcludingId(LegalDocumentType type, String language, String newVersion, Long excludedId) {
        List<LegalDocument> docs = legalDocumentRepository.findAllByTypeAndLanguageOrderByPublishedAtDesc(type, language);
        String latest = null;

        for (LegalDocument doc : docs) {
            if (doc.getId().equals(excludedId)) {
                continue;
            }
            String candidate = doc.getVersion();
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (latest == null || compareVersions(candidate, latest) > 0) {
                latest = candidate;
            }
        }

        if (latest != null && compareVersions(newVersion, latest) <= 0) {
            throw new ValidationException("error.legal.version_not_latest", "LEGAL_VERSION_NOT_LATEST");
        }
    }

    private int compareVersions(String left, String right) {
        int[] leftParts = parseVersionParts(left);
        int[] rightParts = parseVersionParts(right);
        int max = Math.max(leftParts.length, rightParts.length);

        for (int i = 0; i < max; i++) {
            int leftValue = i < leftParts.length ? leftParts[i] : 0;
            int rightValue = i < rightParts.length ? rightParts[i] : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }

        return 0;
    }

    private int[] parseVersionParts(String version) {
        if (version == null || version.isBlank()) {
            throw new ValidationException("error.legal.version_invalid", "LEGAL_VERSION_INVALID");
        }

        String[] parts = version.trim().split("\\.");
        int[] values = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty() || !part.matches("\\d+")) {
                throw new ValidationException("error.legal.version_invalid", "LEGAL_VERSION_INVALID");
            }
            values[i] = Integer.parseInt(part);
        }

        return values;
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
