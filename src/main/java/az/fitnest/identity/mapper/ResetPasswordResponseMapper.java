package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.ResetPasswordResponse;

public final class ResetPasswordResponseMapper {
    private ResetPasswordResponseMapper() {}
    public static ResetPasswordResponse toResponse(String message) {
        return new ResetPasswordResponse(message);
    }
}
