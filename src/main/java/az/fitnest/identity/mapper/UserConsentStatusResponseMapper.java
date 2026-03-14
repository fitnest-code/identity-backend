package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.UserConsentStatusResponse;
import az.fitnest.identity.model.entity.UserConsent;

public final class UserConsentStatusResponseMapper {
    private UserConsentStatusResponseMapper() {}
    public static UserConsentStatusResponse toResponse(UserConsent consent, boolean privacyUpToDate, boolean termsUpToDate) {
        return new UserConsentStatusResponse(
            new UserConsentStatusResponse.ConsentStatus(privacyUpToDate, privacyUpToDate),
            new UserConsentStatusResponse.ConsentStatus(termsUpToDate, termsUpToDate),
            privacyUpToDate && termsUpToDate,
            privacyUpToDate && termsUpToDate,
            consent.getPrivacyPolicyVersion(),
            consent.getAcceptedAt()
        );
    }
}
