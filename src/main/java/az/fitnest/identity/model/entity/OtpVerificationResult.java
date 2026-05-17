package az.fitnest.identity.model.entity;

import az.fitnest.identity.model.enums.OtpPurpose;
import lombok.Builder;

@Builder
public record OtpVerificationResult(
        OtpPurpose purpose,
        String firstName,
        String lastName,
        String passwordHash,
        String mobile,
        String email,
        Long userId
) {
}
