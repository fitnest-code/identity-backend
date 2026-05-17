package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.response.UserConsentStatusResponse;
import az.fitnest.identity.model.entity.UserConsent;
import org.springframework.stereotype.Component;

@Component
public class UserConsentStatusResponseMapper {
    public UserConsentStatusResponse toResponse(UserConsent consent, String latestPrivacyVersion, String latestTermsVersion) {
        boolean privacyUpToDate = latestPrivacyVersion != null && latestPrivacyVersion.equals(consent.getPrivacyPolicyVersion());
        boolean termsUpToDate = latestTermsVersion != null && latestTermsVersion.equals(consent.getTermsOfUseVersion());

        return new UserConsentStatusResponse(
                new UserConsentStatusResponse.ConsentDetail(
                        true,
                        privacyUpToDate,
                        consent.getPrivacyPolicyVersion(),
                        latestPrivacyVersion,
                        consent.getAcceptedAt()
                ),
                new UserConsentStatusResponse.ConsentDetail(
                        true,
                        termsUpToDate,
                        consent.getTermsOfUseVersion(),
                        latestTermsVersion,
                        consent.getAcceptedAt()
                ),
                privacyUpToDate && termsUpToDate
        );
    }
}
