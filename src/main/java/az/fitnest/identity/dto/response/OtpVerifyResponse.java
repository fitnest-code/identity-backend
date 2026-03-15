package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record OtpVerifyResponse(
    Boolean verified,

    @JsonProperty("registration_token")
    String registrationToken,

    @JsonProperty("message")
    String message,

    @JsonProperty("reset_token")
    String resetToken,

    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("refresh_token")
    String refreshToken,

    UserResponse user
) {}
