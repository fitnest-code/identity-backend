package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.AdminConsentResponse;
import az.fitnest.identity.model.entity.UserConsent;
import org.springframework.stereotype.Component;

@Component
public class AdminConsentResponseMapper {
    public AdminConsentResponse toResponse(UserConsent consent) {
        return new AdminConsentResponse(
            consent.getId(),
            consent.getUserId(),
            consent.getPrivacyPolicyVersion(),
            consent.getTermsOfUseVersion(),
            consent.getAcceptedAt(),
            consent.getIpAddress(),
            consent.getUserAgent(),
            consent.getPlatform()
        );
    }
}
