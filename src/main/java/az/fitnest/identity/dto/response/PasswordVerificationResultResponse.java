package az.fitnest.identity.dto.response;

public class PasswordVerificationResultResponse {
    private final boolean matches;
    private final boolean upgradeRecommended;

    public PasswordVerificationResultResponse(boolean matches, boolean upgradeRecommended) {
        this.matches = matches;
        this.upgradeRecommended = upgradeRecommended;
    }

    public boolean matches() {
        return matches;
    }

    public boolean upgradeRecommended() {
        return upgradeRecommended;
    }
}

