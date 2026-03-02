package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OtpSendResponse(
    @JsonProperty("otp_session_id")
    String otpSessionId,

    @JsonProperty("expires_in_seconds")
    Integer expiresInSeconds,

    @JsonProperty("resend_available_in_seconds")
    Integer resendAvailableInSeconds,

    @JsonProperty("message")
    String message
) {}