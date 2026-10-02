package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.entity.Translation;
import az.fitnest.identity.repository.TranslationRepository;
import az.fitnest.identity.service.TranslationService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TranslationServiceImpl implements TranslationService {
    private static final Logger log = LoggerFactory.getLogger(TranslationServiceImpl.class);
    
    private final TranslationRepository translationRepository;
    private final CacheManager cacheManager;

    public TranslationServiceImpl(TranslationRepository translationRepository, CacheManager cacheManager) {
        this.translationRepository = translationRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    @Cacheable(value = "translations", key = "#entityType + '_' + #entityId + '_' + #fieldName + '_' + #languageCode")
    public String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode) {
        if (entityType == null || entityId == null || fieldName == null) {
            return null;
        }
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

        // Manual translations only: AZ lives on its own entity table, EN/RU live in
        // the translations table (admin-provided). No machine translation.
        return null;
    }


    @Override
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
