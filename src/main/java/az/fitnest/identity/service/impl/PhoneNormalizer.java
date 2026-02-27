package az.fitnest.identity.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class PhoneNormalizer {
    private static final Set<String> VALID_OPERATORS =
        Set.of("50", "51", "10", "55", "99", "70", "77", "60");
    private final Cache<String, String> normalizationCache;

    public PhoneNormalizer() {
        this.normalizationCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    }

    public String normalizeAzerbaijanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }
        String cached = normalizationCache.getIfPresent(phoneNumber);
        if (cached != null) {
            return "INVALID".equals(cached) ? null : cached;
        }
        String normalized = doNormalize(phoneNumber);
        normalizationCache.put(phoneNumber, normalized != null ? normalized : "INVALID");
        return normalized;
    }

    private String doNormalize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') sb.append(c);
        }
        String digits = sb.toString();

        String result = null;
        if (digits.startsWith("994") && digits.length() == 12) {
            result = "+" + digits;
        } else if (digits.startsWith("0") && digits.length() == 10) {
            result = "+994" + digits.substring(1);
        } else if (digits.length() == 9) {
            result = "+994" + digits;
        }

        if (result != null && result.length() == 13) {
            String operator = result.substring(4, 6);
            if (VALID_OPERATORS.contains(operator)) {
                return result;
            }
        }
        return null;
    }
}
