package az.fitnest.identity.dto.response;

import az.fitnest.identity.model.entity.User;

public record LoginResult(
        FlowStep flowStep,
        Object payload
) {
    public enum FlowStep {
        SUCCESS,
        REACTIVATION_REQUIRED
    }

    public static LoginResult success(LoginResponse response) {
        return new LoginResult(FlowStep.SUCCESS, response);
    }

    public static LoginResult reactivationRequired(OtpSendResponse response) {
        return new LoginResult(FlowStep.REACTIVATION_REQUIRED, response);
    }
}
