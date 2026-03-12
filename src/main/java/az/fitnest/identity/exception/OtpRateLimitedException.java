package az.fitnest.identity.exception;

import az.fitnest.identity.model.enums.UserStatus;
import org.springframework.http.HttpStatus;

public class OtpRateLimitedException extends BaseException {
    private final long waitTimeSeconds;

    public OtpRateLimitedException(String message, long waitTimeSeconds) {
        super(message, "error.otp.rate_limit_generic", HttpStatus.TOO_MANY_REQUESTS);
        this.waitTimeSeconds = waitTimeSeconds;
    }

    public long getWaitTimeSeconds() {
        return waitTimeSeconds;
    }
}
