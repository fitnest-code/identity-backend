package az.fitnest.identity.exception;

import az.fitnest.identity.model.enums.UserStatus;
import org.springframework.http.HttpStatus;

public class OtpRateLimitedException extends BaseException {
    private final long waitTimeSeconds;

    public OtpRateLimitedException(String message, long waitTimeSeconds) {
        super(message, "RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS);
        this.waitTimeSeconds = waitTimeSeconds;
    }

    public long getWaitTimeSeconds() {
        return waitTimeSeconds;
    }
}