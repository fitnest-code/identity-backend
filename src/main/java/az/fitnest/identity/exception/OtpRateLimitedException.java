package az.fitnest.identity.exception;

import az.fitnest.identity.model.enums.UserStatus;
import org.springframework.http.HttpStatus;

public class OtpRateLimitedException extends BaseException {
    private final long waitTimeSeconds;

    public OtpRateLimitedException(String message, String errorCode, long waitTimeSeconds) {
        super(message, errorCode, HttpStatus.TOO_MANY_REQUESTS);
        this.waitTimeSeconds = waitTimeSeconds;
    }

    public OtpRateLimitedException(String message, long waitTimeSeconds) {
        this(message, "error.otp.rate_limit_generic", waitTimeSeconds);
    }

    public long getWaitTimeSeconds() {
        return waitTimeSeconds;
    }
}
