package az.fitnest.identity.service.impl;

import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.ActiveUserLanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActiveUserLanguageServiceImpl implements ActiveUserLanguageService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserLanguage> findByRoles(List<String> roleNames) {
        List<String> roles = (roleNames == null || roleNames.isEmpty())
                ? List.of("ROLE_USER")
                : roleNames;

        List<Object[]> rows = userRepository.findUserIdsAndLanguagesByRoles(roles);
        List<UserLanguage> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Long userId = (Long) row[0];
            String language = row[1] != null ? row[1].toString() : "AZ";
            result.add(new UserLanguage(userId, language));
        }
        return result;
    }
}
