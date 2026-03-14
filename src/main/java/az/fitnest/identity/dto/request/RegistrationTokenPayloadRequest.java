package az.fitnest.identity.dto.request;

import az.fitnest.identity.model.enums.OtpPurpose;

public class RegistrationTokenPayloadRequest {
    private final String identifier;
    private final OtpPurpose purpose;

    public RegistrationTokenPayloadRequest(String identifier, OtpPurpose purpose) {
        this.identifier = identifier;
        this.purpose = purpose;
    }

    public String getIdentifier() {
        return identifier;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }
}
