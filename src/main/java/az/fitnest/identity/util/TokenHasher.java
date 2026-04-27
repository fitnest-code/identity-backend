package az.fitnest.identity.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * TokenHasher for high-concurrency environments.
 * Uses HMAC-SHA256. Uses ThreadLocal to ensure thread safety
 * and avoid pooling contention.
 */
@Component
public final class TokenHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int MIN_PEPPER_LENGTH_BYTES = 32; // 256 bits

    private final ThreadLocal<Mac> macThreadLocal;
    private final byte[] pepperBytes;

    /**
     * @param pepperBase64 a base64-encoded secret (at least 32 random bytes).
     * @throws IllegalArgumentException if the pepper is missing or too short.
     */
    public TokenHasher(@Value("${auth.token.hash-pepper}") final String pepperBase64) {
        this.pepperBytes = validateAndDecodePepper(pepperBase64);
        this.macThreadLocal = ThreadLocal.withInitial(() -> {
            try {
                Mac mac = Mac.getInstance(ALGORITHM);
                mac.init(new SecretKeySpec(pepperBytes, ALGORITHM));
                return mac;
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException("Failed to initialize HMAC", e);
            }
        });
    }

    /**
     * Computes the HMAC-SHA256 hash of the given token.
     *
     * @param token the token to hash; may be null, in which case null is returned.
     * @return the base64-encoded hash.
     */
    public String hash(final String token) {
        if (token == null) {
            return null;
        }
        byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
        Mac mac = macThreadLocal.get();
        byte[] hash = mac.doFinal(tokenBytes);
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a token against a hash using constant-time comparison to prevent timing attacks.
     *
     * @param token        the token to verify
     * @param existingHash the hash to compare against
     * @return true if the token matches the hash
     */
    public boolean verify(String token, String existingHash) {
        if (token == null || existingHash == null) {
            return false;
        }
        String computedHash = hash(token);
        return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                existingHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static byte[] validateAndDecodePepper(final String pepperBase64) {
        if (pepperBase64 == null || pepperBase64.isBlank()) {
            throw new IllegalArgumentException("HMAC pepper must not be null or blank. Set auth.token.hash-pepper.");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(pepperBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("HMAC pepper must be a valid Base64 string", e);
        }
        if (decoded.length < MIN_PEPPER_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "HMAC pepper must be at least " + MIN_PEPPER_LENGTH_BYTES + " bytes (got " + decoded.length + ")"
            );
        }
        return decoded;
    }
}

