package az.fitnest.identity.util;

import az.fitnest.identity.model.enums.UserStatus;

public class MobileNumberUtils {

    private MobileNumberUtils() {
    }

    public static String normalize(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return null;
        }

        String digits = mobile.replaceAll("\\D", "");

        String normalizedDigits;
        if (digits.startsWith("994") && digits.length() == 12) {
            normalizedDigits = digits;
        } else if (digits.startsWith("0") && digits.length() == 10) {
            normalizedDigits = "994" + digits.substring(1);
        } else if (digits.length() == 9) {
            normalizedDigits = "994" + digits;
        } else {
            return null;
        }

        return "+" + normalizedDigits;
    }
}
