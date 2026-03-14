package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.AdminConsentResponse;
import az.fitnest.identity.model.entity.UserConsent;

public final class AdminConsentResponseMapper {
    private AdminConsentResponseMapper() {}
    public static AdminConsentResponse toResponse(UserConsent consent) {
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
