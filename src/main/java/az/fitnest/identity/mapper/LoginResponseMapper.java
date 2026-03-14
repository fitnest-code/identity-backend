package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.dto.UserResponse;

public final class LoginResponseMapper {
    private LoginResponseMapper() {}
    public static LoginResponse toResponse(String accessToken, String refreshToken, UserResponse userResponse) {
        return new LoginResponse(accessToken, refreshToken, userResponse);
    }
}
