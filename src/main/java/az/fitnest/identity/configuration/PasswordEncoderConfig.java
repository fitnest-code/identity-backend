package az.fitnest.identity.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the password encoding mechanism.
 * Uses DelegatingPasswordEncoder to support multiple algorithms and seamless migration.
 */
@Configuration
@RequiredArgsConstructor
public class PasswordEncoderConfig {

    private final PasswordProperties properties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        
        // BCrypt support for legacy hashes or simple use cases
        encoders.put("bcrypt", new BCryptPasswordEncoder(properties.getBcrypt().getLogRounds()));
        
        // Argon2 support for modern, high-security requirements
        encoders.put("argon2", new Argon2PasswordEncoder(
                properties.getArgon2().getSaltLength(),
                properties.getArgon2().getHashLength(),
                properties.getArgon2().getParallelism(),
                properties.getArgon2().getMemory(),
                properties.getArgon2().getIterations()
        ));

        // Create the delegating encoder with the configured default ID
        DelegatingPasswordEncoder delegatingPasswordEncoder = 
            new DelegatingPasswordEncoder(properties.getDefaultId(), encoders);
        
        // Special case: if no prefix is found, it can be configured to use a default.
        // However, production systems should enforce prefixed hashes for clarity.
        delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(encoders.get("argon2"));

        return delegatingPasswordEncoder;
    }
}