package az.fitnest.iamservice.exception;

public class MethodArgumentNotValidException extends RuntimeException {
    public MethodArgumentNotValidException(String message, Throwable cause) {
        super(message, cause);
    }
}
