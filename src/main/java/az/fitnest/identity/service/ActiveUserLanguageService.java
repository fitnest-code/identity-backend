package az.fitnest.identity.service;

import java.util.List;

/**
 * Returns customer (ROLE_USER) user ids with preferred language for notification fan-out.
 */
public interface ActiveUserLanguageService {

    record UserLanguage(Long userId, String language) {
    }

    List<UserLanguage> findByRoles(List<String> roleNames);
}
