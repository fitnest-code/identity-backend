package az.fitnest.identity.util;
import az.fitnest.identity.model.enums.UserStatus;

public class MobileNumberUtils {

    private MobileNumberUtils() {
        // Private constructor to hide the implicit public one
    }

    public static String normalize(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return null;
        }

        // 1. Remove all non-digit characters
        String digits = mobile.replaceAll("\\D", "");

        // 2. Handle different cases to normalize to 994XXXXXXXXX
        String normalizedDigits;
        if (digits.startsWith("994") && digits.length() == 12) {
            normalizedDigits = digits;
        } else if (digits.startsWith("0") && digits.length() == 10) {
            normalizedDigits = "994" + digits.substring(1);
        } else if (digits.length() == 9) {
            normalizedDigits = "994" + digits;
        } else {
            // Unrecognized format, return as is or null. 
            // Better to return null to fail validation if it doesn't match Azerbaijan pattern.
            return null;
        }

        // 3. Prepend +
        return "+" + normalizedDigits;
    }
}
