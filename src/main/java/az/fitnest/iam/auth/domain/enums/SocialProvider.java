package az.fitnest.iam.auth.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SocialProvider {
    APPLE,
    GOOGLE;

    @JsonCreator
    public static SocialProvider from(String value) {
        if (value == null) {
            return null;
        }
        return SocialProvider.valueOf(value.toUpperCase());
    }
}