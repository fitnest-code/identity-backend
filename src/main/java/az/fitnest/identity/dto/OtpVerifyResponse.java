package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyResponse {

    private Boolean verified;

    @JsonProperty("registration_token")
    private String registrationToken;

    @JsonProperty("message")
    private String message;

    @JsonProperty("reset_token")
    private String resetToken;
}