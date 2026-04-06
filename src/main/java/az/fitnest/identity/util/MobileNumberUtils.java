package az.fitnest.identity.util;

import az.fitnest.identity.model.enums.UserStatus;

public class MobileNumberUtils {

    private static final java.util.Set<String> ALLOWED_PREFIXES = java.util.Set.of("10", "50", "51", "55", "60", "70", "77", "99");

    private MobileNumberUtils() {
    }

    public static String normalize(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return null;
        }

        String digits = mobile.replaceAll("\\D", "");

        String normalizedDigits;
        if (mobile.startsWith("+994") && digits.length() == 12) {
            normalizedDigits = digits;
        } else if (mobile.startsWith("0") && digits.length() == 10) {
            normalizedDigits = "994" + digits.substring(1);
        } else {
            return null;
        }

        String operator = normalizedDigits.substring(3, 5);
        if (!ALLOWED_PREFIXES.contains(operator)) {
            return null;
        }

        return "+" + normalizedDigits;
    }
}
