package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.repository.UserRepository;

import az.fitnest.identity.dto.PasswordVerificationResult;
import az.fitnest.identity.service.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private static final int MAX_PASSWORD_LENGTH = 128;

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public String hashPassword(String rawPassword) {
        validatePassword(rawPassword);
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public PasswordVerificationResult verifyPassword(String rawPassword, String passwordHash) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(passwordHash)) {
            return new PasswordVerificationResult(false, false);
        }

        try {
            boolean matches = passwordEncoder.matches(rawPassword, passwordHash);

            boolean upgradeRecommended = matches && passwordEncoder.upgradeEncoding(passwordHash);

            return new PasswordVerificationResult(matches, upgradeRecommended);
        } catch (Exception e) {
            return new PasswordVerificationResult(false, false);
        }
    }

    @Override
    public boolean isStrongPassword(String password) {
        if (password == null) return false;
        return password.length() >= 8 &&
               password.matches(".*[A-Z].*") &&
               password.matches(".*[a-z].*") &&
               password.matches(".*\\d.*") &&
               password.matches(".*[^A-Za-z0-9].*");
    }

    @Override
    public boolean isPasswordReused(Long userId, String newPassword) {
        az.fitnest.identity.model.entity.User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        return passwordEncoder.matches(newPassword, user.getPasswordHash());
    }

    private void validatePassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password exceeds maximum allowed length");
        }
    }
}
