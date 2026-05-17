package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank
        @JsonProperty("otp_session_id")
        String otpSessionId,

        @NotBlank
        @Pattern(regexp = "^\\d{4}$")
        @JsonProperty("otp_code")
        String otpCode
) {
}
