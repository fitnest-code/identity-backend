package az.fitnest.identity.configuration;

import az.fitnest.identity.model.enums.UserStatus;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.security.password")
public class PasswordProperties {

    private String defaultId = "argon2";

    private Bcrypt bcrypt = new Bcrypt();

    private Argon2 argon2 = new Argon2();

    @Getter
    @Setter
    public static class Bcrypt {
        private int logRounds = 12;
    }

    @Getter
    @Setter
    public static class Argon2 {
        private int memory = 65536;

        private int iterations = 3;

        private int parallelism = 2;

        private int saltLength = 16;

        private int hashLength = 32;
    }
}
