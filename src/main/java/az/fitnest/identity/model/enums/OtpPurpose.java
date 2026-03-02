package az.fitnest.identity.model.enums;

import az.fitnest.identity.model.enums.UserStatus;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OtpPurpose {
    REGISTRATION,
    LOGIN,
    PASSWORD_RESET,
    REACTIVATION,
    EMAIL_CHANGE,
    MOBILE_CHANGE;

    @JsonCreator
    public static OtpPurpose from(String value) {
        if (value == null) return null;

        try {
            return OtpPurpose.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid otp purpose: " + value
            );
        }
    }
}