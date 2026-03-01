package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import java.security.SecureRandom;
import java.util.Base64;

public class OtpSessionIdGenerator {

    private static final int BYTES_LENGTH = 16;
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSessionId() {
        byte[] randomBytes = new byte[BYTES_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return BASE64_URL_ENCODER.encodeToString(randomBytes);
    }
}
