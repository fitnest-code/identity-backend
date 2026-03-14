package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.ResetPasswordResponse;
import org.springframework.stereotype.Component;

@Component
public class ResetPasswordResponseMapper {
    public ResetPasswordResponse toResponse(String message) {
        return new ResetPasswordResponse(message);
    }
}
