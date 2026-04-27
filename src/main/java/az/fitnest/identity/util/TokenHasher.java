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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Optimized TokenHasher for high-concurrency environments (Virtual Thread friendly).
 * Uses HMAC-SHA256 with a pooled approach to minimize allocation overhead and avoid ThreadLocal leaks.
 */
@Component
public final class TokenHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int MIN_PEPPER_LENGTH_BYTES = 32; // 256 bits
    // Small pool sized to the number of CPU cores to handle bursts without ThreadLocal overhead
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    private final SecretKeySpec secretKey;
    private final BlockingQueue<Mac> macPool;

    public TokenHasher(@Value("${auth.token.hash-pepper:default-pepper-must-be-changed-in-prod-long-enough}") final String pepperBase64) {
        byte[] pepperBytes = validateAndDecodePepper(pepperBase64);
        this.secretKey = new SecretKeySpec(pepperBytes, ALGORITHM);
        this.macPool = new ArrayBlockingQueue<>(POOL_SIZE);

        // Pre-initialize the pool for instant readiness
        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                Mac mac = Mac.getInstance(ALGORITHM);
                mac.init(secretKey);
                macPool.offer(mac);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new IllegalStateException("Failed to initialize cryptographic provider", e);
            }
        }
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

        Mac mac = macPool.poll();
        try {
            if (mac == null) {
                // Fallback for extreme bursts: create a new one (safe for virtual threads)
                mac = Mac.getInstance(ALGORITHM);
                mac.init(secretKey);
            }
            byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
            byte[] hash = mac.doFinal(tokenBytes);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Token hashing failed", e);
        } finally {
            if (mac != null) {
                macPool.offer(mac); // Return to pool
            }
        }
    }

    /**
     * Verifies a token against a hash using constant-time comparison to prevent timing attacks.
     *
     * @param token the token to verify
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
            throw new IllegalArgumentException("HMAC pepper must not be null or blank");
        }
        byte[] decoded = Base64.getDecoder().decode(pepperBase64);
        if (decoded.length < MIN_PEPPER_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                "HMAC pepper must be at least " + MIN_PEPPER_LENGTH_BYTES + " bytes (got " + decoded.length + ")"
            );
        }
        return decoded;
    }
}
