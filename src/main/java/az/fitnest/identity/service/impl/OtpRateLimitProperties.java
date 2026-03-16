package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

import javax.annotation.PostConstruct;

@Validated
@Getter
@Setter
@ToString
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "otp.rate-limit")
public class OtpRateLimitProperties {
    @Min(1)
    private int maxAttempts = 5;
    @Min(1)
    private int windowMinutes = 10;
    @Min(0)
    private int cooldownSeconds = 60;
    @Min(1)
    private int dailyMaxAttempts = 10;
    private boolean failOpen = false;
    @Min(100)
    private long minExpiryMs = 1000;

    public long getWindowMillis() {
        return windowMinutes * 60L * 1000L;
    }

    public long getWindowSeconds() {
        return windowMinutes * 60L;
    }

    public long getCooldownMillis() {
        return cooldownSeconds * 1000L;
    }

    @PostConstruct
    public void validate() {
        if (cooldownSeconds > 0 && cooldownSeconds >= windowMinutes * 60) {
            throw new IllegalStateException(
                    "Cooldown period cannot be longer than window duration"
            );
        }
    }
}
