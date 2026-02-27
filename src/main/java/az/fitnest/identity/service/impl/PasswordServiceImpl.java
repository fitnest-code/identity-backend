package az.fitnest.identity.service.impl;

import az.fitnest.identity.dto.PasswordVerificationResult;
import az.fitnest.identity.service.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Production-grade implementation of {@link PasswordService}.
 * Features:
 * - DelegatingPasswordEncoder support for multiple algorithms.
 * - Argon2 as the current high-security default.
 * - BCrypt support for legacy compatibility.
 * - Maximum password length enforcement to prevent DoS attacks.
 * - Constant-time verification behavior.
 * - Transparent hash upgrade detection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    /**
     * Maximum password length to prevent excessive CPU usage during hashing (DoS protection).
     * Argon2/BCrypt can be computationally expensive for very long strings.
     */
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hashPassword(String rawPassword) {
        validatePassword(rawPassword);
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public PasswordVerificationResult verifyPassword(String rawPassword, String passwordHash) {
        // Uniform failure for blank inputs to mitigate enumeration and timing leakage
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(passwordHash)) {
            log.debug("Verification failed due to blank input");
            return new PasswordVerificationResult(false, false);
        }

        try {
            // matches() typically uses constant-time comparisons internally
            boolean matches = passwordEncoder.matches(rawPassword, passwordHash);
            
            // Check if the current hash uses a suboptimal algorithm or parameters
            boolean upgradeRecommended = matches && passwordEncoder.upgradeEncoding(passwordHash);

            return new PasswordVerificationResult(matches, upgradeRecommended);
        } catch (Exception e) {
            // Catch unexpected hashing failures (e.g. malformed hash) to prevent stack trace leaks
            log.error("Error during password verification: {}", e.getMessage());
            return new PasswordVerificationResult(false, false);
        }
    }

    private void validatePassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (rawPassword.length() > MAX_PASSWORD_LENGTH) {
            log.warn("Password hash attempt blocked: length {} exceeds limit {}", 
                    rawPassword.length(), MAX_PASSWORD_LENGTH);
            throw new IllegalArgumentException("Password exceeds maximum allowed length");
        }
    }
}
