package az.fitnest.identity.configuration;

import az.fitnest.identity.model.enums.UserStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PasswordEncoderConfig {

    private final PasswordProperties properties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();

        encoders.put("bcrypt", new BCryptPasswordEncoder(properties.getBcrypt().getLogRounds()));

        encoders.put("argon2", new Argon2PasswordEncoder(
                properties.getArgon2().getSaltLength(),
                properties.getArgon2().getHashLength(),
                properties.getArgon2().getParallelism(),
                properties.getArgon2().getMemory(),
                properties.getArgon2().getIterations()
        ));

        DelegatingPasswordEncoder delegatingPasswordEncoder =
                new DelegatingPasswordEncoder(properties.getDefaultId(), encoders);

        delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(encoders.get("argon2"));

        return delegatingPasswordEncoder;
    }
}
