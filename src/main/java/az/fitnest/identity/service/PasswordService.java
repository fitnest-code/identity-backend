package az.fitnest.identity.service;

import az.fitnest.identity.dto.PasswordVerificationResult;

/**
 * Service for secure password hashing and verification.
 */
public interface PasswordService {
    
    /**
     * Hashes a raw password using the current default secure algorithm.
     */
    String hashPassword(String rawPassword);

    /**
     * Verifies a raw password against an encoded hash and detects if an upgrade is needed.
     */
    PasswordVerificationResult verifyPassword(String rawPassword, String passwordHash);
}
