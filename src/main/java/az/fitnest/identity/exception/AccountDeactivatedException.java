package az.fitnest.identity.exception;

import lombok.Getter;

@Getter
public class AccountDeactivatedException extends BaseException {
    private final String otpSessionId;

    public AccountDeactivatedException(String message, String otpSessionId) {
        super(message, org.springframework.http.HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED");
        this.otpSessionId = otpSessionId;
    }
}
