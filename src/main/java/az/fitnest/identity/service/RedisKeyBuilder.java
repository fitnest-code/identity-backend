package az.fitnest.identity.service;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Component
public final class RedisKeyBuilder {

    private static final String VERSION = "v1";
    private static final String SEPARATOR = ":";

    public static final String PREFIX_OTP = "otp";
    public static final String PREFIX_SESSION = "session";
    public static final String PREFIX_COOLDOWN = "cooldown";
    public static final String PREFIX_ACTIVE = "active";
    public static final String PREFIX_RL = "rl";

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final String pepper;
    private final Map<OtpPurpose, String> purposePrefixes = new EnumMap<>(OtpPurpose.class);
    private final ThreadLocal<Mac> macThreadLocal = new ThreadLocal<>();

    public record RedisKeys(String windowKey, String cooldownKey) {}

    public RedisKeyBuilder(@Value("${otp.rate-limit.key-hmac-secret:#{null}}") String secret,
                          @Value("${otp.key-builder.pepper:change-me-in-production}") String pepper) {
        this.pepper = secret != null ? secret : pepper;
    }

    @PostConstruct
    public void init() {
        for (OtpPurpose purpose : OtpPurpose.values()) {
            purposePrefixes.put(purpose, purpose.name());
        }

        try {
            Mac.getInstance(HMAC_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 algorithm not available", e);
        }
    }

    public String sessionKey(String sessionId) {
        validateInput("sessionId", sessionId);
        return buildKey(PREFIX_OTP, VERSION, PREFIX_SESSION, hash(sessionId));
    }

    public String cooldownKey(OtpPurpose purpose, String email) {
        validateInputs(purpose, email);
        return buildKey(PREFIX_OTP, VERSION, PREFIX_COOLDOWN, purposePrefixes.get(purpose), hashEmail(email));
    }

    public String activeSessionKey(OtpPurpose purpose, String email) {
        validateInputs(purpose, email);
        return buildKey(PREFIX_OTP, VERSION, PREFIX_ACTIVE, purposePrefixes.get(purpose), hashEmail(email));
    }

    public RedisKeys rateLimitKeys(OtpPurpose purpose, String identifier) {
        validateInputs(purpose, identifier);
        // stable, non-PII identifier
        String id = shortHmac(purpose.name() + "|" + identifier);
        String tag = "{" + id + "}"; // same slot for Redis Cluster
        return new RedisKeys(
            PREFIX_OTP + SEPARATOR + PREFIX_RL + SEPARATOR + tag + ":w",
            PREFIX_OTP + SEPARATOR + PREFIX_RL + SEPARATOR + tag + ":c"
        );
    }

    public String rateLimitAttemptsKey(OtpPurpose purpose, String email) {
        validateInputs(purpose, email);
        return buildKey(PREFIX_OTP, VERSION, PREFIX_RL, purposePrefixes.get(purpose), hashEmail(email), "attempts");
    }

    public String rateLimitLastAttemptKey(OtpPurpose purpose, String email) {
        validateInputs(purpose, email);
        return buildKey(PREFIX_OTP, VERSION, PREFIX_RL, purposePrefixes.get(purpose), hashEmail(email), "last");
    }

    public String getSessionKeyPrefix() {
        return buildKey(PREFIX_OTP, VERSION, PREFIX_SESSION, "");
    }

    private String hashEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return hash(normalizedEmail);
    }

    private String hash(String input) {
        try {
            Mac mac = getMac();
            byte[] hashBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL_ENCODER.encodeToString(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash input", e);
        }
    }

    private String shortHmac(String s) {
        try {
            Mac mac = getMac();
            byte[] h = mac.doFinal(s.getBytes(StandardCharsets.UTF_8));
            // 12 bytes => 24 hex chars (plenty for entropy, avoids long keys)
            return HexFormat.of().formatHex(java.util.Arrays.copyOf(h, 12));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate HMAC", e);
        }
    }

    private Mac getMac() throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = macThreadLocal.get();
        if (mac == null) {
            mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            macThreadLocal.set(mac);
        }
        return mac;
    }

    private String buildKey(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part != null) {
                if (i > 0) {
                    sb.append(SEPARATOR);
                }
                sb.append(part);
            }
        }
        return sb.toString();
    }

    private void validateInputs(OtpPurpose purpose, String email) {
        if (purpose == null) {
            throw new IllegalArgumentException("Purpose cannot be null");
        }
        validateInput("email/identifier", email);
    }

    private void validateInput(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
    }
}
