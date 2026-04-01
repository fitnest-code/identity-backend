package az.fitnest.identity.dto.request;

import az.fitnest.identity.model.enums.OtpPurpose;
import lombok.Data;

@Data
public class OtpRateLimitResetRequest {
    private OtpPurpose purpose;
    private String identifier;
}
