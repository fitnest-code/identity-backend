package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationTokenPayload {
    private String identifier;
    private OtpPurpose purpose;
}

