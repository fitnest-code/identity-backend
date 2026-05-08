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

        // 1. Remove all non-digits
        String digits = mobile.replaceAll("\\D", "");

        String normalizedDigits;
        
        // 2. Handle 994551234567 or +994551234567 (12 digits)
        if (digits.length() == 12 && digits.startsWith("994")) {
            normalizedDigits = digits;
        } 
        // 3. Handle 0551234567 (10 digits)
        else if (digits.length() == 10 && digits.startsWith("0")) {
            normalizedDigits = "994" + digits.substring(1);
        }
        // 4. Handle 551234567 (9 digits)
        else if (digits.length() == 9) {
            normalizedDigits = "994" + digits;
        }
        else {
            return null;
        }

        // 5. Check operator prefix (positions 3 and 4 in 994XXYYYYYY)
        String operator = normalizedDigits.substring(3, 5);
        if (!ALLOWED_PREFIXES.contains(operator)) {
            return null;
        }

        return "+" + normalizedDigits;
    }
}
