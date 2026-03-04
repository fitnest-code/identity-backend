package az.fitnest.identity.exception;
 
import org.springframework.http.HttpStatus;
 
public class InvalidCredentialsException extends BaseException {
 
    private static final long serialVersionUID = 1L;
 
    public InvalidCredentialsException(String message) {
        super(message, "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
    }

    public InvalidCredentialsException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED);
    }
}
