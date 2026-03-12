package az.fitnest.identity.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message, "error.resource.not_found", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, String errorCode) {
        super(message, errorCode.startsWith("error.") ? errorCode : "error.resource.not_found", HttpStatus.NOT_FOUND);
    }
}
