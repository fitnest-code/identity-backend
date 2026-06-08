package az.fitnest.identity.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.FORBIDDEN);
    }
}
