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

        LegalDocument doc = legalDocumentRepository.findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(type, normalizedLang)
                .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("Sənəd tapılmadı"));

        return legalDocumentResponseMapper.toResponse(doc, type);
    }

    @Transactional
    @Override
    public void createDocument(CreateLegalDocumentRequest request) {
        if (legalDocumentRepository.existsByTypeAndVersion(request.type(), request.version())) {
            throw new az.fitnest.identity.exception.ConflictException("Sənəd versiyası artıq mövcuddur");
        }

        String normalizedLang = normalizeLanguage(request.language());

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
    public void autoAcceptLatestConsents(Long userId) {
        String latestPrivacyVersion = getLatestVersion(LegalDocumentType.PRIVACY_POLICY);
        String latestTermsVersion = getLatestVersion(LegalDocumentType.TERMS_OF_USE);

        if (latestPrivacyVersion == null || latestTermsVersion == null) {
            return;
        }

        UserConsent consent = UserConsent.builder()
                .userId(userId)
                .privacyPolicyVersion(latestPrivacyVersion)
                .termsOfUseVersion(latestTermsVersion)
                .acceptedAt(LocalDateTime.now())
                .platform("SYSTEM")
                .ipAddress("0.0.0.0")
                .userAgent("SYSTEM-AUTO-ACCEPT")
                .build();

        userConsentRepository.save(consent);
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

        return consents.map(c -> adminConsentResponseMapper.toResponse(c));
    }

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
