package az.fitnest.iam.auth.api.dto.response;

import az.fitnest.iam.user.api.dto.response.UserResponse;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString(exclude = {"accessToken", "refreshToken"})
public class LoginResponse {

    @JsonAlias("access_token")
    private final String accessToken;

    @JsonAlias("refresh_token")
    private final String refreshToken;

    private final UserResponse user;
}