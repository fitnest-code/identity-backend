package az.fitnest.iam.otp.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendResponse {

    @JsonProperty("otp_session_id")
    private String otpSessionId;

    @JsonProperty("expires_in_seconds")
    private Integer expiresInSeconds;

    @JsonProperty("resend_available_in_seconds")
    private Integer resendAvailableInSeconds;

    @JsonProperty("message")
    private String message;
}