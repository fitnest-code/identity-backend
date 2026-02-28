package az.fitnest.identity.constants;
import az.fitnest.identity.model.enums.UserStatus;

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