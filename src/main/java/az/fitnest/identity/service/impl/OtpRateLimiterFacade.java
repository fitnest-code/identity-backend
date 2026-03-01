package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OtpRateLimiterFacade {
    private final ResilientOtpRateLimiter resilientRateLimiter;
    private final DeduplicatingOtpRateLimiter deduplicatingRateLimiter;
    private final MeterRegistry meterRegistry;
    private final boolean useAsync;

    public OtpRateLimiterFacade(
            ResilientOtpRateLimiter resilientRateLimiter,
            DeduplicatingOtpRateLimiter deduplicatingRateLimiter,
            MeterRegistry meterRegistry,
            @Value("${otp.ratelimiter.async:false}") boolean useAsync) {
        this.resilientRateLimiter = resilientRateLimiter;
        this.deduplicatingRateLimiter = deduplicatingRateLimiter;
        this.meterRegistry = meterRegistry;
        this.useAsync = useAsync;
    }

    public OtpRateLimiter.RateLimitResult checkRateLimit(OtpPurpose purpose, String phoneNumber, String clientIp) {
        if (useAsync) {
            try {
                return deduplicatingRateLimiter.checkRateLimitAsync(purpose, phoneNumber, clientIp)
                        .get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                meterRegistry.counter("otp.ratelimit.async.timeout").increment();
                return resilientRateLimiter.checkRateLimit(purpose, phoneNumber, clientIp);
            }
        }
        return resilientRateLimiter.checkRateLimit(purpose, phoneNumber, clientIp);
    }

    public OtpRateLimiter.RateLimitResult checkRateLimit(OtpPurpose purpose, String phoneNumber) {
        return checkRateLimit(purpose, phoneNumber, null);
    }
}
