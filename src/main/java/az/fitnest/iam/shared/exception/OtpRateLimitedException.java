package az.fitnest.iam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class OtpRateLimitedException extends RuntimeException {
    public OtpRateLimitedException(String message) {
        super(message);
    }
}