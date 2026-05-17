package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing JWT tokens and user information after successful login")
public record LoginResponse(
        @JsonProperty("access_token")
        @Schema(description = "JWT access token for API authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @JsonProperty("refresh_token")
        @Schema(description = "JWT refresh token to obtain new access tokens", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken,

        @Schema(description = "User profile information")
        UserResponse user
) {
}
