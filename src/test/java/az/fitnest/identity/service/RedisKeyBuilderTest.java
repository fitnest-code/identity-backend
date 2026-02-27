package az.fitnest.identity.service;

import az.fitnest.identity.constants.OtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedisKeyBuilderTest {

    private RedisKeyBuilder redisKeyBuilder;
    private final String pepper = "test-pepper-123";

    @BeforeEach
    void setUp() {
        redisKeyBuilder = new RedisKeyBuilder(pepper);
        redisKeyBuilder.init();
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
    void normalization_shouldProduceSameHash_forSameEmailDifferentSpacing() {
        String email1 = " user@example.com   ";
        String email2 = "user@example.com";
        
        String key1 = redisKeyBuilder.cooldownKey(OtpPurpose.LOGIN, email1);
        String key2 = redisKeyBuilder.cooldownKey(OtpPurpose.LOGIN, email2);
        
        assertEquals(key1, key2, "Keys should be identical for trimmed email");
    }

    @Test
    void keyedHashing_shouldProduceDifferentHashes_forDifferentPepper() {
        String email = "user@example.com";
        String key1 = redisKeyBuilder.cooldownKey(OtpPurpose.LOGIN, email);
        
        RedisKeyBuilder builder2 = new RedisKeyBuilder("different-pepper");
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

    @Test
    void keyFormat_shouldBeV1() {
        String email = "user@example.com";
        String key = redisKeyBuilder.cooldownKey(OtpPurpose.LOGIN, email);
        
        assertTrue(key.startsWith("otp:v1:cooldown:LOGIN:"), "Key should follow v1 template");
    }

    @Test
    void validation_shouldThrowOnNullPurpose() {
        assertThrows(IllegalArgumentException.class, 
            () -> redisKeyBuilder.cooldownKey(null, "user@example.com"));
    }

    @Test
    void validation_shouldThrowOnNullEmail() {
        assertThrows(IllegalArgumentException.class, 
            () -> redisKeyBuilder.cooldownKey(OtpPurpose.LOGIN, null));
    }

    @Test
    void validation_shouldThrowOnBlankEmail() {
        assertThrows(IllegalArgumentException.class, 
            () -> redisKeyBuilder.cooldownKey(OtpPurpose.LOGIN, "   "));
    }

    @Test
    void getSessionKeyPrefix_shouldMatchSessionKeyBase() {
        String prefix = redisKeyBuilder.getSessionKeyPrefix();
        String sessionId = "abc";
        String key = redisKeyBuilder.sessionKey(sessionId);
        
        assertTrue(key.startsWith(prefix), "Session key should start with the shared prefix");
        assertEquals("otp:v1:session:", prefix, "Prefix should match expected format");
    }
}
