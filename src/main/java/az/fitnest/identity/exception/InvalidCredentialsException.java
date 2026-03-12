package az.fitnest.identity.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BaseException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException(String message) {
        super(message, "error.auth.invalid_credentials", HttpStatus.UNAUTHORIZED);
    }

    public InvalidCredentialsException(String message, String errorCode) {
        super(message, errorCode.startsWith("error.") ? errorCode : "error.auth.invalid_credentials", HttpStatus.UNAUTHORIZED);
    }
}
