package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.OtpVerifyResponse;
import az.fitnest.identity.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class OtpVerifyResponseMapper {
    public OtpVerifyResponse toResponse(boolean success, String registrationToken, String message, String resetToken, String accessToken, String refreshToken, UserResponse user) {
        return new OtpVerifyResponse(success, registrationToken, message, resetToken, accessToken, refreshToken, user);
    }
}
