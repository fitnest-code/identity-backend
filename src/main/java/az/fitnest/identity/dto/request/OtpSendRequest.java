package az.fitnest.identity.dto.request;

import az.fitnest.identity.model.enums.OtpPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Schema(description = "Request to send an OTP code to a mobile number")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpSendRequest {
    @NotNull
    @Schema(description = "Purpose of the OTP (REGISTRATION, LOGIN, PASSWORD_RESET, REACTIVATION, EMAIL_CHANGE, MOBILE_CHANGE)", example = "REGISTRATION", requiredMode = Schema.RequiredMode.REQUIRED)
    private OtpPurpose purpose;

    @Schema(description = "Mobile number to receive the OTP", example = "0501234567")
    @jakarta.validation.constraints.Pattern(regexp = "^(0|\\+994|994)?(10|50|51|55|60|70|77|99)\\d{7}$", message = "Invalid mobile number format. Must be in Azerbaijan format (e.g., 0501234567 or +994501234567).")
    private String mobile;

    @Email
    @Schema(description = "Email address to receive the OTP", example = "user@example.com")
    private String email;

    @Schema(description = "Session ID for the OTP request", example = "abc123sessionid")
    private String sessionId;

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
