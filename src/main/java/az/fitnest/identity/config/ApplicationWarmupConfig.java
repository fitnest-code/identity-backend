package az.fitnest.identity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class ApplicationWarmupConfig {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.warmup.enabled:true}")
    private boolean warmupEnabled;

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
            log.info("Application warmup is disabled");
            return;
        }

        log.info("Starting application warmup...");
        long startTime = System.currentTimeMillis();

        // Warm up database connection pool
        if (warmupDb) {
            warmupDatabase();
        }

        // Warm up Redis connection
        warmupRedis();

        // Warm up JIT by touching commonly used classes
        warmupJit();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Application warmup completed in {}ms", elapsed);
    }

    /**
     * Warm up database connection pool by executing a simple query.
     * This ensures connections are established before user requests arrive.
     */
    private void warmupDatabase() {
        try {
            log.debug("Warming up database connection pool...");
            long start = System.currentTimeMillis();

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

            log.debug("Database warmup completed in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Failed to warm up database: {}", e.getMessage());
        }
    }

    /**
     * Warm up Redis connection.
     */
    private void warmupRedis() {
        try {
            log.debug("Warming up Redis connection...");
            long start = System.currentTimeMillis();

            // Simple ping to establish connection
            redisTemplate.hasKey("__warmup__");

            log.debug("Redis warmup completed in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Failed to warm up Redis: {}", e.getMessage());
        }
    }

    /**
     * Warm up JIT compiler by touching commonly used code paths.
     */
    private void warmupJit() {
        try {
            log.debug("Warming up JIT...");
            long start = System.currentTimeMillis();

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

            log.debug("JIT warmup completed in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Failed JIT warmup: {}", e.getMessage());
        }
    }
}
