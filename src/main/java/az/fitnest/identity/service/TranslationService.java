package az.fitnest.identity.service;

public interface TranslationService {
    String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode);
    void saveOrUpdateTranslation(String entityType, String entityId, String languageCode, String fieldName, String fieldValue);
}
