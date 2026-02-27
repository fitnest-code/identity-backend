package az.fitnest.identity.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration properties for password hashing.
 * Allows fine-tuning of BCrypt and Argon2 parameters for production environments.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.security.password")
public class PasswordProperties {

    /**
     * The ID of the default password encoder to use for new hashes.
     * Common values: argon2, bcrypt.
     */
    private String defaultId = "argon2";

    /**
     * BCrypt specific configurations.
     */
    private Bcrypt bcrypt = new Bcrypt();

    /**
     * Argon2 specific configurations.
     */
    private Argon2 argon2 = new Argon2();

    @Getter
    @Setter
    public static class Bcrypt {
        /**
         * Log rounds for BCrypt (between 4 and 31).
         * Production recommended: 10-12+.
         */
        private int logRounds = 12;
    }

    @Getter
    @Setter
    public static class Argon2 {
        /**
         * Memory cost in KB (2^n).
         * Production recommended: 65536 (64MB).
         */
        private int memory = 65536;

        /**
         * Number of iterations.
         */
        private int iterations = 3;

        /**
         * Degree of parallelism.
         */
        private int parallelism = 2;

        /**
         * Salt length in bytes.
         */
        private int saltLength = 16;

        /**
         * Hash length in bytes.
         */
        private int hashLength = 32;
    }
}
