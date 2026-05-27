package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.entity.Translation;
import az.fitnest.identity.repository.TranslationRepository;
import az.fitnest.identity.service.TranslationService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;

@Service
public class TranslationServiceImpl implements TranslationService {
    private static final Logger log = LoggerFactory.getLogger(TranslationServiceImpl.class);
    
    private final TranslationRepository translationRepository;
    private final CacheManager cacheManager;
    private final RestTemplate restTemplate;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private TranslationServiceImpl self;

    @org.springframework.beans.factory.annotation.Autowired
    private TranslationEntityResolver translationEntityResolver;

    public TranslationServiceImpl(TranslationRepository translationRepository, CacheManager cacheManager) {
        this.translationRepository = translationRepository;
        this.cacheManager = cacheManager;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1500);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    @Cacheable(value = "translations", key = "#entityType + '_' + #entityId + '_' + #fieldName + '_' + #languageCode")
    public String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode) {
        if (languageCode == null || languageCode.equalsIgnoreCase("AZ")) {
            return null;
        }

        if (entityType != null) {
            String normType = entityType.toUpperCase();
            if (normType.equals("USER_STATUS") || normType.equals("USERSTATUS")) {
                String status = entityId.toUpperCase();
                if (languageCode.equalsIgnoreCase("EN")) {
                    switch (status) {
                        case "ACTIVE": return "Active";
                        case "INACTIVE": return "Inactive";
                        case "LOCKED": return "Locked";
                        case "BLOCKED": return "Blocked";
                        case "DELETED": return "Deleted";
                        default: return entityId;
                    }
                } else if (languageCode.equalsIgnoreCase("RU")) {
                    switch (status) {
                        case "ACTIVE": return "Активный";
                        case "INACTIVE": return "Неактивный";
                        case "LOCKED": return "Заблокировано";
                        case "BLOCKED": return "Заблокирован";
                        case "DELETED": return "Удалено";
                        default: return entityId;
                    }
                }
                return entityId;
            } else if (normType.equals("OTP_VERIFICATION_STATUS") || normType.equals("OTPVERIFICATIONSTATUS")) {
                String status = entityId.toUpperCase();
                if (languageCode.equalsIgnoreCase("EN")) {
                    switch (status) {
                        case "VERIFIED": return "Verified";
                        case "NOT_VERIFIED": return "Not Verified";
                        case "EXPIRED": return "Expired";
                        default: return entityId;
                    }
                } else if (languageCode.equalsIgnoreCase("RU")) {
                    switch (status) {
                        case "VERIFIED": return "Подтверждено";
                        case "NOT_VERIFIED": return "Не подтверждено";
                        case "EXPIRED": return "Истек";
                        default: return entityId;
                    }
                }
                return entityId;
            } else if (normType.equals("SESSION_STATUS") || normType.equals("SESSIONSTATUS")) {
                String status = entityId.toUpperCase();
                if (languageCode.equalsIgnoreCase("EN")) {
                    switch (status) {
                        case "ACTIVE": return "Active";
                        case "INACTIVE": return "Inactive";
                        default: return entityId;
                    }
                } else if (languageCode.equalsIgnoreCase("RU")) {
                    switch (status) {
                        case "ACTIVE": return "Активный";
                        case "INACTIVE": return "Неактивный";
                        default: return entityId;
                    }
                }
                return entityId;
            }
        }

        String existingValue = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                entityType.toUpperCase(),
                entityId,
                languageCode.toUpperCase(),
                fieldName
        )
        .map(Translation::getFieldValue)
        .orElse(null);

        if (existingValue != null) {
            return existingValue;
        }

        try {
            Class<?> entityClass = translationEntityResolver.getEntityClass(entityType);
            if (entityClass != null) {
                Object entity = null;
                try {
                    Long longId = Long.parseLong(entityId);
                    entity = entityManager.find(entityClass, longId);
                } catch (NumberFormatException e) {
                    entity = entityManager.find(entityClass, entityId);
                }

                if (entity != null) {
                    String originalValueAz = translationEntityResolver.extractFieldValue(entity, fieldName);
                    if (originalValueAz != null && !originalValueAz.trim().isEmpty()) {
                        String translatedValue = translateText(originalValueAz, languageCode.toLowerCase());
                        if (translatedValue != null && !translatedValue.trim().isEmpty()) {
                            self.saveOrUpdateTranslation(entityType, entityId, languageCode, fieldName, translatedValue);
                            return translatedValue;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Soft fallback translation failed for entityType={}, entityId={}, fieldName={}, lang={}",
                    entityType, entityId, fieldName, languageCode, e);
        }

        return null;
    }

    @Override
    @Async
    @CacheEvict(value = "translations", allEntries = true)
    public void autoTranslateAndSave(String entityType, String entityId, String fieldName, String originalValueAz) {
        if (originalValueAz == null || originalValueAz.trim().isEmpty()) {
            log.warn("Auto-translation skipped: originalValueAz is null or empty for entityType={}, entityId={}, fieldName={}", 
                entityType, entityId, fieldName);
            return;
        }

        log.info("Starting auto-translation process for entityType={}, entityId={}, fieldName={}, originalValueAz='{}'", 
            entityType, entityId, fieldName, originalValueAz);

        // Translate to EN
        String enValue = translateText(originalValueAz, "en");
        if (enValue != null && !enValue.trim().isEmpty()) {
            log.info("Auto-translated [AZ -> EN] success. Value: '{}'", enValue);
            saveOrUpdateTranslation(entityType, entityId, "EN", fieldName, enValue);
        } else {
            log.warn("Auto-translation [AZ -> EN] returned empty or null value. Using fallback: '{}'", originalValueAz);
            saveOrUpdateTranslation(entityType, entityId, "EN", fieldName, originalValueAz);
        }

        // Translate to RU
        String ruValue = translateText(originalValueAz, "ru");
        if (ruValue != null && !ruValue.trim().isEmpty()) {
            log.info("Auto-translated [AZ -> RU] success. Value: '{}'", ruValue);
            saveOrUpdateTranslation(entityType, entityId, "RU", fieldName, ruValue);
        } else {
            log.warn("Auto-translation [AZ -> RU] returned empty or null value. Using fallback: '{}'", originalValueAz);
            saveOrUpdateTranslation(entityType, entityId, "RU", fieldName, originalValueAz);
        }
    }

    private String translateText(String text, String targetLanguage) {
        try {
            String googleTranslated = translateWithGoogle(text, targetLanguage);
            if (googleTranslated != null && !googleTranslated.trim().isEmpty()) {
                log.info("Translation successful using Google Translate [AZ -> {}]: '{}' -> '{}'", 
                    targetLanguage.toUpperCase(), text, googleTranslated);
                return googleTranslated;
            }
        } catch (Exception e) {
            log.error("Google Translate failed. Error: {}", e.getMessage());
        }
        return null;
    }

    private String translateWithGoogle(String text, String targetLanguage) {
        try {
            URI uri = UriComponentsBuilder
                .fromUriString("https://translate.googleapis.com/translate_a/single")
                .queryParam("client", "gtx")
                .queryParam("sl", "az")
                .queryParam("tl", targetLanguage.toLowerCase())
                .queryParam("dt", "t")
                .queryParam("q", text)
                .build()
                .toUri();

            log.info("Google Translate Request [AZ -> {}]: '{}'", targetLanguage.toUpperCase(), text);
            String response = restTemplate.getForObject(uri, String.class);
            if (response != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response);
                if (rootNode.isArray() && rootNode.size() > 0) {
                    JsonNode firstArray = rootNode.get(0);
                    if (firstArray.isArray() && firstArray.size() > 0) {
                        StringBuilder translatedText = new StringBuilder();
                        for (JsonNode pair : firstArray) {
                            if (pair.isArray() && pair.size() > 0) {
                                translatedText.append(pair.get(0).asText());
                            }
                        }
                        return translatedText.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Google Translation API failed for text '{}' to '{}': {}", text, targetLanguage, e.getMessage());
        }
        return null;
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void saveOrUpdateTranslation(String entityType, String entityId, String languageCode, String fieldName, String fieldValue) {
        String normalizedEntityType = entityType.toUpperCase();
        String normalizedLanguageCode = languageCode.toUpperCase();

        log.info("Database Save: entityType={}, entityId={}, languageCode={}, fieldName={}, fieldValue='{}'", 
            normalizedEntityType, entityId, normalizedLanguageCode, fieldName, fieldValue);

        Translation existing = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                normalizedEntityType, entityId, normalizedLanguageCode, fieldName
        ).orElse(null);

        if (existing != null) {
            log.info("Updating existing translation record ID={}", existing.getId());
            existing.setFieldValue(fieldValue);
            translationRepository.save(existing);
        } else {
            log.info("Creating new translation record");
            Translation translation = Translation.builder()
                    .entityType(normalizedEntityType)
                    .entityId(entityId)
                    .languageCode(normalizedLanguageCode)
                    .fieldName(fieldName)
                    .fieldValue(fieldValue)
                    .build();
            translationRepository.save(translation);
        }

        evictCache(normalizedEntityType, entityId, fieldName, normalizedLanguageCode);
    }

    private void evictCache(String entityType, String entityId, String fieldName, String languageCode) {
        if (cacheManager != null) {
            try {
                org.springframework.cache.Cache cache = cacheManager.getCache("translations");
                if (cache != null) {
                    String key = entityType + "_" + entityId + "_" + fieldName + "_" + languageCode;
                    cache.evict(key);
                    log.info("Evicted translation cache for key: {}", key);
                }
            } catch (Exception e) {
                log.error("Failed to evict cache: {}", e.getMessage());
            }
        }
    }
}
