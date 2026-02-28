package az.fitnest.identity.exception;
import az.fitnest.identity.model.enums.UserStatus;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseException {
    
    private static final long serialVersionUID = 1L;
    
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }
    
    public ConflictException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }
}
