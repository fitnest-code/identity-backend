package az.fitnest.iam.legal.adapter.persistence;

import az.fitnest.iam.legal.domain.enums.LegalDocumentType;
import az.fitnest.iam.legal.domain.model.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {
    Optional<LegalDocument> findTopByTypeAndLanguageAndIsActiveTrueOrderByPublishedAtDesc(LegalDocumentType type, String language);
    Optional<LegalDocument> findTopByTypeAndIsActiveTrueOrderByPublishedAtDesc(LegalDocumentType type);
    boolean existsByTypeAndVersionAndIsActiveTrue(LegalDocumentType type, String version);
    boolean existsByTypeAndVersion(LegalDocumentType type, String version);
    java.util.List<LegalDocument> findAllByTypeAndLanguageAndIsActiveTrue(LegalDocumentType type, String language);
}
