package az.fitnest.identity.service;

import az.fitnest.identity.constants.OtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedisKeyBuilderTest {

    private final String pepper = "test-pepper-123";
    private RedisKeyBuilder redisKeyBuilder;

    @BeforeEach
    void setUp() {
        redisKeyBuilder = new RedisKeyBuilder(null, pepper);
        redisKeyBuilder.init();
    }

    @Test
    void rateLimitKeys_shouldUseHashtags_and_Hmac() {
        String identifier = "+994501234567";
        RedisKeyBuilder.RedisKeys keys = redisKeyBuilder.rateLimitKeys(OtpPurpose.LOGIN, identifier);

        // Example: otp:rl:{...}:w
        assertTrue(keys.windowKey().startsWith("otp:rl:{"), "Window key should have hashtag tag");
        assertTrue(keys.windowKey().endsWith(":w"), "Window key should end with :w");
        assertTrue(keys.cooldownKey().endsWith(":c"), "Cooldown key should end with :c");

        String tag1 = extractTag(keys.windowKey());
        String tag2 = extractTag(keys.cooldownKey());

        assertEquals(tag1, tag2, "Both keys must have the same tag for Redis Cluster slotting");
        assertFalse(tag1.contains(identifier), "Tag should be hashed, not contain raw PII");
    }

    @Test
    void rateLimitKeys_shouldBeStable_forSameIdentifier() {
        String id = "some-id";
        String tag1 = extractTag(redisKeyBuilder.rateLimitKeys(OtpPurpose.LOGIN, id).windowKey());
        String tag2 = extractTag(redisKeyBuilder.rateLimitKeys(OtpPurpose.LOGIN, id).windowKey());

        assertEquals(tag1, tag2, "HMAC must be stable");
    }

    private String extractTag(String key) {
        int start = key.indexOf('{');
        int end = key.indexOf('}');
        return key.substring(start + 1, end);
    }

    @Test
    void normalization_shouldProduceSameHash_forSameEmailDifferentCase() {
        String email1 = "User@Example.com ";
        String email2 = "user@example.com";

        String key1 = redisKeyBuilder.cooldownKey(OtpPurpose.LOGIN, email1);
        String key2 = redisKeyBuilder.cooldownKey(OtpPurpose.LOGIN, email2);

        assertEquals(key1, key2, "Keys should be identical for normalized email");
    }

    @Test
    void keyedHashing_shouldProduceDifferentHashes_forDifferentPepper() {
        String email = "user@example.com";
        String key1 = redisKeyBuilder.cooldownKey(OtpPurpose.LOGIN, email);

        RedisKeyBuilder builder2 = new RedisKeyBuilder(null, "different-pepper");
        builder2.init();
        String key2 = builder2.cooldownKey(OtpPurpose.LOGIN, email);

        assertNotEquals(key1, key2, "Keys should be different with different peppers");
    }

    @Test
    void sessionKey_shouldBeHashed() {
        String sessionId = "some-long-session-id-123";
        String key = redisKeyBuilder.sessionKey(sessionId);

        assertTrue(key.startsWith("otp:v1:session:"), "Key should have correct prefix");
        assertFalse(key.contains(sessionId), "Key should not contain raw session ID (PII/Leaking)");
    }
}
