package az.fitnest.identity.model.entity;

import az.fitnest.identity.model.enums.OtpPurpose;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtpSessionPayload(
    OtpPurpose purpose,
    String otpHash,
    Integer attempts,
    Boolean locked,
    Boolean verified,
    Instant createdAt,
    String firstName,
    String lastName,
    String userPasswordHash,
    String mobile,
    String email,
    Instant lockedUntil,
    Long userId
) {}
