package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.service.OtpRateLimiter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;
import az.fitnest.identity.model.enums.OtpPurpose;
import io.lettuce.core.RedisException;

import java.time.Duration;

@Component
public class ResilientOtpRateLimiter {
    private final az.fitnest.identity.service.impl.OtpRateLimiter delegate;
    private final CircuitBreaker circuitBreaker;
    private final MeterRegistry meterRegistry;

    public ResilientOtpRateLimiter(az.fitnest.identity.service.impl.OtpRateLimiter delegate, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(10)
                .slidingWindowSize(100)
                .recordExceptions(
                        RedisConnectionFailureException.class,
                        RedisException.class,
                        DataAccessResourceFailureException.class,
                        org.springframework.data.redis.RedisSystemException.class
                )
                .build();
        this.circuitBreaker = CircuitBreaker.of("otpRateLimiter", config);
    }

    public az.fitnest.identity.service.impl.OtpRateLimiter.RateLimitResult checkRateLimit(OtpPurpose purpose, String phoneNumber, String clientIp) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                return delegate.checkRateLimit(purpose, phoneNumber, clientIp);
            } catch (Exception e) {
                meterRegistry.counter("otp.ratelimit.circuitbreaker.failure").increment();
                throw e;
            }
        });
    }

    public az.fitnest.identity.service.impl.OtpRateLimiter.RateLimitResult checkRateLimit(OtpPurpose purpose, String phoneNumber) {
        return checkRateLimit(purpose, phoneNumber, null);
    }
}
