package az.fitnest.identity.service;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.service.*;
import java.util.Locale;
import org.springframework.stereotype.Service;

public interface EmailNormalizationService {
    String normalize(String email);
}
