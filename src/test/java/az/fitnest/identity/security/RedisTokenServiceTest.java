package az.fitnest.identity.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.DefaultTypedTuple;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Exercises the rotation-grace semantics against an in-memory ZSET fake (member -> score) so the
 * score arithmetic is verified without a Redis instance.
 */
class RedisTokenServiceTest {

    private static final long GRACE = 45;

    private final Map<String, Double> zset = new TreeMap<>();
    private RedisTokenService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> ops = mock(ZSetOperations.class);
        when(template.opsForZSet()).thenReturn(ops);
        when(template.type(anyString())).thenAnswer(i -> zset.isEmpty() ? DataType.NONE : DataType.ZSET);

        when(ops.add(anyString(), anyString(), anyDouble())).thenAnswer(i -> {
            zset.put(i.getArgument(1), i.getArgument(2));
            return true;
        });
        when(ops.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenAnswer(i -> {
            double max = i.getArgument(2);
            long before = zset.size();
            zset.values().removeIf(score -> score <= max);
            return before - zset.size();
        });
        // any(Object.class): bare any() infers Object[] and binds to the multi-score varargs overload.
        when(ops.score(anyString(), any(Object.class))).thenAnswer(i -> zset.get((String) i.getArgument(1)));
        when(ops.rangeWithScores(anyString(), anyLong(), anyLong())).thenAnswer(i -> {
            Set<ZSetOperations.TypedTuple<String>> out = new LinkedHashSet<>();
            zset.forEach((m, s) -> out.add(new DefaultTypedTuple<>(m, s)));
            return out;
        });

        service = new RedisTokenService(template, "auth:token:access:", "auth:user:session:", GRACE);
    }

    @Test
    void rotation_keepsPreviousJtiValidDuringGraceOnly_andNewJtiUntilRefreshExpiry() {
        service.setActiveSession(1L, "Web", "jti-old", Duration.ofDays(7));
        service.setActiveSession(1L, "Web", "jti-new", Duration.ofDays(7));

        long now = Instant.now().getEpochSecond();
        assertTrue(service.isSessionValid(1L, "Web", "jti-old"), "previous JTI must survive rotation");
        assertTrue(service.isSessionValid(1L, "Web", "jti-new"));

        double oldScore = zset.get("jti-old");
        double newScore = zset.get("jti-new");
        assertTrue(oldScore <= now + GRACE + 1 && oldScore > now, "old JTI limited to the grace window");
        assertTrue(newScore > now + Duration.ofDays(6).getSeconds(), "new JTI keeps full refresh lifetime");

        // Simulate the grace window elapsing: old JTI is pruned, new one remains.
        zset.put("jti-old", (double) now - 1);
        assertFalse(service.isSessionValid(1L, "Web", "jti-old"));
        assertTrue(service.isSessionValid(1L, "Web", "jti-new"));
    }

    @Test
    void rotation_neverExtendsAMemberAlreadyInsideGrace() {
        service.setActiveSession(1L, "Web", "jti-1", Duration.ofDays(7));
        service.setActiveSession(1L, "Web", "jti-2", Duration.ofDays(7));
        double firstDeadline = zset.get("jti-1");

        service.setActiveSession(1L, "Web", "jti-3", Duration.ofDays(7));

        assertTrue(zset.get("jti-1") <= firstDeadline, "grace deadline must only ever shrink");
        assertTrue(zset.get("jti-2") <= Instant.now().getEpochSecond() + GRACE + 1);
    }
}
