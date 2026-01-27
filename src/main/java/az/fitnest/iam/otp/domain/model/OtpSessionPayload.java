package az.fitnest.iam.otp.domain.model;

import az.fitnest.iam.otp.domain.enums.OtpPurpose;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpSessionPayload {

    private String email;

    private OtpPurpose purpose;

    private String otpHash;

    private Integer attempts;

    private Boolean locked;

    private Boolean verified;

    private Instant createdAt;

    private Boolean emailExistsAtCreation;
}