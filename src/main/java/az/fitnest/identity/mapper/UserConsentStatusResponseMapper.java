package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.response.UserConsentStatusResponse;
import az.fitnest.identity.model.entity.UserConsent;
import org.springframework.stereotype.Component;

@Component
public class UserConsentStatusResponseMapper {
    public UserConsentStatusResponse toResponse(UserConsent consent, boolean privacyUpToDate, boolean termsUpToDate) {
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
