package az.fitnest.iam.shared.util;

public class MobileNumberUtils {

    private MobileNumberUtils() {
        // Private constructor to hide the implicit public one
    }

    public static String normalize(String mobile) {
        if (mobile == null) {
            return null;
        }
        // If already starts with +994, assume it's normalized (legacy support/safety)
        if (mobile.startsWith("+994")) {
            return mobile;
        }
        // If starts with 0 (e.g., 050...), remove 0 and prepend +994
        if (mobile.startsWith("0")) {
            return "+994" + mobile.substring(1);
        }
        // Fallback: prepend +994 if it looks like a raw number (50xxxxxxx)
        return "+994" + mobile;
    }
}
