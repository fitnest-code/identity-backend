package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "OTP göndərilməsi cavabı")
public record OtpSendResponse(
        @JsonProperty("otp_session_id")
        @Schema(description = "OTP sessiya ID-si", example = "550e8400-e29b-41d4-a716-446655440000")
        String otpSessionId,

        @JsonProperty("expires_in_seconds")
        @Schema(description = "OTP-nin etibarlılıq vaxtı (saniyə)", example = "180")
        Integer expiresInSeconds,

        @JsonProperty("resend_available_in_seconds")
        @Schema(description = "Yenidən göndərmə üçün gözləmə vaxtı (saniyə)", example = "60")
        Integer resendAvailableInSeconds,

        @JsonProperty("message")
        @Schema(description = "Məlumat mesajı", example = "Təsdiq kodu göndərildi")
        String message
) {
}
