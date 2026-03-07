package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.PasswordVerificationResult;

public interface PasswordService {

    String hashPassword(String rawPassword);

    PasswordVerificationResult verifyPassword(String rawPassword, String passwordHash);
}
