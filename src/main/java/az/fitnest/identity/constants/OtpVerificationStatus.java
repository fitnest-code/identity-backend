package az.fitnest.identity.constants;
import az.fitnest.identity.model.enums.UserStatus;

public enum OtpVerificationStatus {
    NOT_FOUND,
    LOCKED,
    ALREADY_VERIFIED,
    EXPIRED,
    SUCCESS
}
