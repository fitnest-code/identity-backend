package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.response.PasswordVerificationResultResponse;

public interface PasswordService {

    String hashPassword(String rawPassword);

    PasswordVerificationResultResponse verifyPassword(String rawPassword, String passwordHash);

    boolean isStrongPassword(String password);

    boolean isPasswordReused(Long userId, String newPassword);
}
