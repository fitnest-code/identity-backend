package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.dto.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class LoginResponseMapper {
    public LoginResponse toResponse(String accessToken, String refreshToken, UserResponse userResponse) {
        return new LoginResponse(accessToken, refreshToken, userResponse);
    }
}
