package az.fitnest.identity.exception;

import az.fitnest.identity.model.enums.UserStatus;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class InvalidCredentialsException extends BaseException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
    }
}
