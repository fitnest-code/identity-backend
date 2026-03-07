package az.fitnest.identity.repository;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.LegalDocumentType;
import az.fitnest.identity.model.entity.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {
    Optional<LegalDocument> findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(LegalDocumentType type, String language);

    Optional<LegalDocument> findTopByTypeAndIsActiveTrueOrderByPublishedAtDesc(LegalDocumentType type);

    boolean existsByTypeAndVersionAndIsActiveTrue(LegalDocumentType type, String version);

    boolean existsByTypeAndVersion(LegalDocumentType type, String version);

    List<LegalDocument> findAllByTypeAndLanguageAndIsActiveTrue(LegalDocumentType type, String language);

    List<LegalDocument> findAllByOrderByPublishedAtDesc();

    List<LegalDocument> findAllByTypeOrderByPublishedAtDesc(LegalDocumentType type);

    List<LegalDocument> findAllByTypeAndLanguageOrderByPublishedAtDesc(LegalDocumentType type, String language);

    List<LegalDocument> findAllByIsActiveOrderByPublishedAtDesc(boolean isActive);

    List<LegalDocument> findAllByTypeAndIsActiveOrderByPublishedAtDesc(LegalDocumentType type, boolean isActive);
}
