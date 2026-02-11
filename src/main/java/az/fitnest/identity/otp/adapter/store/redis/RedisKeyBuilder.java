package az.fitnest.identity.otp.adapter.store.redis;

import az.fitnest.identity.constants.OtpPurpose;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class RedisKeyBuilder {

    private static final String PREFIX = "otp";
    private static final String SESSION = PREFIX + ":session:";
    private static final String COOLDOWN = PREFIX + ":cooldown:";
    private static final String ACTIVE = PREFIX + ":active:";
    private static final String RL = PREFIX + ":rl:";

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    public String sessionKey(String sessionId) {
        return SESSION + sessionId;
    }

    public String cooldownKey(OtpPurpose purpose, String email) {
        return COOLDOWN + purpose.name() + ":" + hashEmail(email);
    }

    public String activeSessionKey(OtpPurpose purpose, String email) {
        return ACTIVE + purpose.name() + ":" + hashEmail(email);
    }

    public String rateLimitAttemptsKey(OtpPurpose purpose, String email) {
        return RL + purpose.name() + ":" + hashEmail(email) + ":attempts";
    }

    public String rateLimitLastAttemptKey(OtpPurpose purpose, String email) {
        return RL + purpose.name() + ":" + hashEmail(email) + ":last";
    }

    public String getSessionKeyPrefix() {
        return SESSION;
    }

    private String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(email.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL_ENCODER.encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
