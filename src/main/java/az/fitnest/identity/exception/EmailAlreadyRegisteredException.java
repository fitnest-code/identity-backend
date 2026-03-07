package az.fitnest.identity.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends BaseException {

    private static final long serialVersionUID = 1L;

    public EmailAlreadyRegisteredException(String message) {
        super(message, "EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT);
    }
}
