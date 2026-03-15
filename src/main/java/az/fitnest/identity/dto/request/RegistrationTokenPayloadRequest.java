package az.fitnest.identity.dto.request;

import az.fitnest.identity.model.enums.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationTokenPayloadRequest {
    private String identifier;
    private OtpPurpose purpose;
}
