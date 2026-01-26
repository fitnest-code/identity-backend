package az.fitnest.iam.user.adapter.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class EmailNormalizationService {

    public String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
