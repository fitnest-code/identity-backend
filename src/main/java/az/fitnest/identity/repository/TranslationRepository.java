package az.fitnest.identity.repository;

import az.fitnest.identity.model.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TranslationRepository extends JpaRepository<Translation, Long> {
    Optional<Translation> findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(String entityType, String entityId, String languageCode, String fieldName);
}
