package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

/**
 * Result of a password verification attempt.
 *
 * @param matches            true if the raw password matches the stored hash
 * @param upgradeRecommended true if the password should be re-hashed using the current default algorithm or parameters
 */
public record PasswordVerificationResult(boolean matches, boolean upgradeRecommended) {
}
