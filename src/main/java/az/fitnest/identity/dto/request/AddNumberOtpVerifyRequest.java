package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddNumberOtpVerifyRequest(
        @NotBlank(message = "OTP session ID is required")
        @JsonProperty("otp_session_id")
        String otpSessionId,

        @NotBlank(message = "OTP code is required")
        @Pattern(regexp = "^\\d{4}$", message = "OTP code must be 4 digits")
        @JsonProperty("otp_code")
        String otpCode,

        @JsonProperty("device_id")
        String deviceId,

        @NotBlank(message = "Cihaz tipi tələb olunur")
        @Pattern(regexp = "^(iOS|Android|Web)$", message = "Yanlış cihaz tipi. iOS, Android və ya Web olmalıdır.")
        @JsonProperty("device_type")
        String deviceType
) {}
