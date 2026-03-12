package az.fitnest.identity.exception;

import az.fitnest.identity.model.enums.UserStatus;

import lombok.Getter;

@Getter
public class AccountDeactivatedException extends BaseException {
    private final String otpSessionId;

    public AccountDeactivatedException(String message, String otpSessionId) {
        super(message, "error.account.deactivated", org.springframework.http.HttpStatus.FORBIDDEN);
        this.otpSessionId = otpSessionId;
    }
}
