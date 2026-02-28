package az.fitnest.identity.configuration;
import az.fitnest.identity.model.enums.UserStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Application warmup configuration that pre-warms various resources at startup.
 * This helps eliminate cold-start latency on the first requests.
 */
@Configuration
public class ApplicationWarmupConfig {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.warmup.enabled:true}")
    private boolean warmupEnabled;

    public ApplicationWarmupConfig(DataSource dataSource, RedisTemplate<String, Object> redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @Value("${app.warmup.db:true}")
    private boolean warmupDb;

    /**
     * Warm up application resources after startup.
     * Runs asynchronously to not block the application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmupApplication() {
        if (!warmupEnabled) {
            return;
        }


        // Warm up database connection pool
        if (warmupDb) {
            warmupDatabase();
        }

        // Warm up Redis connection
        warmupRedis();

        // Warm up JIT by touching commonly used classes
        warmupJit();
    }

    /**
     * Warm up database connection pool by executing a simple query.
     * This ensures connections are established before user requests arrive.
     */
    private void warmupDatabase() {
        try {
            // Execute multiple queries to warm up multiple connections in the pool
            for (int i = 0; i < 3; i++) {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        rs.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * Warm up Redis connection.
     */
    private void warmupRedis() {
        try {
            // Simple ping to establish connection
            redisTemplate.hasKey("__warmup__");
        } catch (Exception e) {
        }
    }

    /**
     * Warm up JIT compiler by touching commonly used code paths.
     */
    private void warmupJit() {
        try {
            // Touch common string operations
            String test = "warmup-test-string";
            test.toLowerCase();
            test.toUpperCase();
            test.split("-");

            // Touch common collections
            java.util.List<String> list = new java.util.ArrayList<>();
            list.add("test");
            list.stream().filter(s -> s != null).count();

            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("key", "value");
            map.get("key");
        } catch (Exception e) {
        }
    }
}
