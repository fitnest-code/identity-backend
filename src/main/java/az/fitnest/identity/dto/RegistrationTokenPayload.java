package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.OtpPurpose;

public record RegistrationTokenPayload(
    String identifier,
    OtpPurpose purpose
) {}
