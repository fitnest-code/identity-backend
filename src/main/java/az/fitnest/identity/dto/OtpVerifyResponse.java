package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
) {
}
