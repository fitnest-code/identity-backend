package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.LegalDocumentType;
import az.fitnest.identity.dto.request.*;
import az.fitnest.identity.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LegalService {
    LegalDocumentResponse getPrivacyPolicy(String lang, String format);

    LegalDocumentResponse getTermsOfUse(String lang, String format);

    void createDocument(CreateLegalDocumentRequest request);

    void acceptConsent(Long userId, ConsentAcceptRequest request, String ipAddress, String userAgent);

    void autoAcceptLatestConsents(Long userId);

    UserConsentStatusResponse getUserConsentStatus(Long userId);

    boolean isConsentRequired(Long userId);

    List<AdminLegalDocumentResponse> getAllDocuments(LegalDocumentType type, String language, Boolean active);

    AdminLegalDocumentResponse getDocumentById(Long id);

    AdminLegalDocumentResponse updateDocument(Long id, UpdateLegalDocumentRequest request);

    void deleteDocument(Long id);

    void activateDocument(Long id);

    void deactivateDocument(Long id);

    Page<AdminConsentResponse> getConsents(Long userId, Pageable pageable);
}
