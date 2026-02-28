package az.fitnest.identity.model.entity;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.constants.OtpPurpose;
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
