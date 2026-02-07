package az.fitnest.iam.shared.exception;

import org.springframework.http.HttpStatus;

public class OtpRateLimitedException extends BaseException {
    public OtpRateLimitedException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
    }
}