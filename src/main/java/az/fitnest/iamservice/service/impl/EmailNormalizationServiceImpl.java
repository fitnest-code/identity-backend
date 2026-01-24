package az.fitnest.iamservice.service.impl;

import az.fitnest.iamservice.service.EmailNormalizationService;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class EmailNormalizationServiceImpl implements EmailNormalizationService {

    @Override
    public String normalize(String email) {
        if (email == null) return null;

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
