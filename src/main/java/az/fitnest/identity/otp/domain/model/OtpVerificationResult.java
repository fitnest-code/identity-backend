package az.fitnest.identity.otp.domain.model;

import az.fitnest.identity.otp.domain.enums.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationResult {

    private OtpPurpose purpose;
    private String firstName;
    private String lastName;
    private String passwordHash;
    private String mobile;
}
