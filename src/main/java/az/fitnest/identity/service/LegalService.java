package az.fitnest.identity.service;

import az.fitnest.identity.constants.LegalDocumentType;
import az.fitnest.identity.dto.ConsentAcceptRequest;
import az.fitnest.identity.dto.CreateLegalDocumentRequest;
import az.fitnest.identity.dto.LegalDocumentResponse;
import az.fitnest.identity.dto.UserConsentStatusResponse;
import az.fitnest.identity.entity.UserConsent;
import az.fitnest.identity.exception.ValidationException;
import az.fitnest.identity.repository.UserConsentRepository;
import az.fitnest.identity.service.*;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface LegalService {
    LegalDocumentResponse getPrivacyPolicy(String lang, String format);
    LegalDocumentResponse getTermsOfUse(String lang, String format);
    void createDocument(CreateLegalDocumentRequest request);
    void acceptConsent(Long userId, ConsentAcceptRequest request, String ipAddress, String userAgent);
    UserConsentStatusResponse getUserConsentStatus(Long userId);
    boolean isConsentRequired(Long userId);
}
