package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
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
