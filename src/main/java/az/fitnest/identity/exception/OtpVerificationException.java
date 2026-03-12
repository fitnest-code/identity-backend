package az.fitnest.identity.exception;

import org.springframework.http.HttpStatus;

public class OtpVerificationException extends BaseException {
    private static final long serialVersionUID = 1L;
    public OtpVerificationException(String errorCode) {
        super(errorCode, errorCode, HttpStatus.BAD_REQUEST);
    }
}

