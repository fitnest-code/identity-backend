package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpVerifyRequest {

    @NotBlank
    @JsonProperty("otp_session_id")
    private String otpSessionId;

    @NotBlank
    @Pattern(regexp = "^\\d{4}$")
    @JsonProperty("otp_code")
    private String otpCode;
}