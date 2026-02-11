package az.fitnest.identity.service.impl;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class EmailNormalizationServiceImpl implements EmailNormalizationService {

        @Override
    public String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
