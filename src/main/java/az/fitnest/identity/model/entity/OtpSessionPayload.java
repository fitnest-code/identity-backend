package az.fitnest.identity.model.entity;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.constants.OtpPurpose;
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



    private OtpPurpose purpose;

    private String otpHash;

    private Integer attempts;

    private Boolean locked;

    private Boolean verified;

    private Instant createdAt;



    // Temporary registration data
    private String firstName;
    private String lastName;
    private String userPasswordHash;
    private String mobile;
}