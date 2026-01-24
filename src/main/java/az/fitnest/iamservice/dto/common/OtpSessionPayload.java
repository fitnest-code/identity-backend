package az.fitnest.iamservice.dto.common;

import az.fitnest.iamservice.enums.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSessionPayload {

    private String email;
    private OtpPurpose purpose;
    private String otpHash;
    private int attempts;
    private boolean locked;
    private boolean verified;
    private Instant createdAt;
}

