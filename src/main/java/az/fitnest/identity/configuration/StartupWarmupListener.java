package az.fitnest.identity.configuration;

import az.fitnest.identity.model.enums.UserStatus;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.warmup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StartupWarmupListener {

    private final JdbcTemplate jdbcTemplate;

    public StartupWarmupListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        warmupDatabase();
    }

    private void warmupDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
        }
    }
}
