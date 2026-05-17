package az.fitnest.identity.dto;

public record PasswordVerificationResult(
        boolean matches,
        boolean upgradeRecommended
) {
}
