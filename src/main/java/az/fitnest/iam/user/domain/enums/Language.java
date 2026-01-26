package az.fitnest.iam.user.domain.enums;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum Language {
    AZ,
    EN,
    RU;

    @JsonCreator
    public static Language from(String value) {
        if (value == null) return null;
        return Language.valueOf(value.toUpperCase());
    }
}