package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.dto.request.RegistrationTokenPayloadRequest;

public interface RegistrationTokenService {
    String issueForIdentifier(String identifier);

    RegistrationTokenPayloadRequest requirePayload(String token);

    String requireIdentifier(String token);

    void consume(String token);
}
