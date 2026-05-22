package az.fitnest.identity.service;

import az.fitnest.identity.dto.request.CreateLegalDocumentRequest;
import az.fitnest.identity.dto.request.UpdateLegalDocumentRequest;
import az.fitnest.identity.dto.response.LegalDocumentResponse;
import az.fitnest.identity.mapper.AdminConsentResponseMapper;
import az.fitnest.identity.mapper.LegalDocumentResponseMapper;
import az.fitnest.identity.mapper.UserConsentStatusResponseMapper;
import az.fitnest.identity.model.entity.LegalDocument;
import az.fitnest.identity.model.enums.LegalDocumentType;
import az.fitnest.identity.repository.LegalDocumentRepository;
import az.fitnest.identity.repository.UserConsentRepository;
import az.fitnest.identity.service.impl.LegalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LegalServiceImplTest {

    @Mock
    private UserConsentRepository userConsentRepository;
    @Mock
    private LegalDocumentRepository legalDocumentRepository;
    @Mock
    private LegalDocumentResponseMapper legalDocumentResponseMapper;
    @Mock
    private UserConsentStatusResponseMapper userConsentStatusResponseMapper;
    @Mock
    private AdminConsentResponseMapper adminConsentResponseMapper;
    @Mock
    private TranslationService translationService;

    private LegalServiceImpl legalService;

    @BeforeEach
    void setUp() {
        legalService = new LegalServiceImpl(
                userConsentRepository,
                legalDocumentRepository,
                legalDocumentResponseMapper,
                userConsentStatusResponseMapper,
                adminConsentResponseMapper,
                translationService
        );
    }

    @Test
    void createDocument_shouldTriggerAutoTranslation_whenLanguageIsAz() {
        CreateLegalDocumentRequest request = new CreateLegalDocumentRequest(
                LegalDocumentType.PRIVACY_POLICY,
                "1.0",
                "AZ",
                "Azerbaijan Content",
                false
        );

        when(legalDocumentRepository.existsByTypeAndLanguageAndVersion(any(), anyString(), anyString())).thenReturn(false);
        when(legalDocumentRepository.save(any(LegalDocument.class))).thenAnswer(invocation -> {
            LegalDocument doc = invocation.getArgument(0);
            doc.setId(123L); // Simulate generated ID
            return doc;
        });

        legalService.createDocument(request);

        verify(legalDocumentRepository).save(any(LegalDocument.class));
        verify(translationService).autoTranslateAndSave("LEGAL_DOCUMENT", "123", "content", "Azerbaijan Content");
    }

    @Test
    void createDocument_shouldNotTriggerAutoTranslation_whenLanguageIsNotAz() {
        CreateLegalDocumentRequest request = new CreateLegalDocumentRequest(
                LegalDocumentType.PRIVACY_POLICY,
                "1.0",
                "EN",
                "English Content",
                false
        );

        when(legalDocumentRepository.existsByTypeAndLanguageAndVersion(any(), anyString(), anyString())).thenReturn(false);
        when(legalDocumentRepository.save(any(LegalDocument.class))).thenAnswer(invocation -> {
            LegalDocument doc = invocation.getArgument(0);
            doc.setId(124L);
            return doc;
        });

        legalService.createDocument(request);

        verify(legalDocumentRepository).save(any(LegalDocument.class));
        verify(translationService, never()).autoTranslateAndSave(any(), any(), any(), any());
    }

    @Test
    void updateDocument_shouldTriggerAutoTranslation_whenLanguageIsAz() {
        UpdateLegalDocumentRequest request = new UpdateLegalDocumentRequest(
                "1.1",
                "AZ",
                "Updated Az Content"
        );

        LegalDocument existingDoc = LegalDocument.builder()
                .type(LegalDocumentType.PRIVACY_POLICY)
                .version("1.0")
                .language("AZ")
                .content("Old Content")
                .isActive(true)
                .build();
        existingDoc.setId(555L);

        when(legalDocumentRepository.findById(555L)).thenReturn(Optional.of(existingDoc));
        when(legalDocumentRepository.save(any(LegalDocument.class))).thenReturn(existingDoc);

        legalService.updateDocument(555L, request);

        verify(legalDocumentRepository).save(existingDoc);
        verify(translationService).autoTranslateAndSave("LEGAL_DOCUMENT", "555", "content", "Updated Az Content");
    }

    @Test
    void getPrivacyPolicy_shouldFetchAzAndTranslate_whenRequestedLangIsNotAz() {
        LegalDocument azDoc = LegalDocument.builder()
                .type(LegalDocumentType.PRIVACY_POLICY)
                .version("1.0")
                .language("AZ")
                .content("Az Məzmunu")
                .isActive(true)
                .publishedAt(LocalDateTime.now())
                .build();
        azDoc.setId(999L);

        when(legalDocumentRepository.findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(
                LegalDocumentType.PRIVACY_POLICY, "AZ"
        )).thenReturn(Optional.of(azDoc));

        when(translationService.getTranslatedValue("LEGAL_DOCUMENT", "999", "content", "EN"))
                .thenReturn("English Translated Content");

        LegalDocumentResponse response = legalService.getPrivacyPolicy("EN", "html");

        assertNotNull(response);
        assertEquals("English Translated Content", response.content());
        assertEquals("1.0", response.version());
        assertEquals("PRIVACY_POLICY", response.title());
    }

    @Test
    void getPrivacyPolicy_shouldFallBackToOriginalContent_whenTranslationIsMissing() {
        LegalDocument azDoc = LegalDocument.builder()
                .type(LegalDocumentType.PRIVACY_POLICY)
                .version("1.0")
                .language("AZ")
                .content("Az Məzmunu")
                .isActive(true)
                .publishedAt(LocalDateTime.now())
                .build();
        azDoc.setId(999L);

        when(legalDocumentRepository.findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(
                LegalDocumentType.PRIVACY_POLICY, "AZ"
        )).thenReturn(Optional.of(azDoc));

        when(translationService.getTranslatedValue("LEGAL_DOCUMENT", "999", "content", "RU"))
                .thenReturn(null); // No translation found

        LegalDocumentResponse response = legalService.getPrivacyPolicy("RU", "html");

        assertNotNull(response);
        assertEquals("Az Məzmunu", response.content()); // Falls back to AZ content
        assertEquals("1.0", response.version());
    }
}
