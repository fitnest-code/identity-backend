package az.fitnest.identity.service.impl;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class DeduplicatingOtpRateLimiter {
    private final OtpRateLimiter delegate;
    private final Cache<String, CompletableFuture<OtpRateLimiter.RateLimitResult>> requestCache;
    private final ExecutorService rlExecutor;

    public DeduplicatingOtpRateLimiter(OtpRateLimiter delegate) {
        this.delegate = delegate;
        this.rlExecutor = Executors.newFixedThreadPool(16,
            new ThreadFactoryBuilder().setNameFormat("otp-rl-%d").build());
        this.requestCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(200, TimeUnit.MILLISECONDS)
            .build();
    }

    public CompletableFuture<OtpRateLimiter.RateLimitResult> checkRateLimitAsync(
            OtpPurpose purpose, String phoneNumber, String clientIp) {
        String cacheKey = (purpose.name() + ":" + phoneNumber + ":" + (clientIp != null ? clientIp : "none"));
        return requestCache.get(cacheKey, k ->
            CompletableFuture.supplyAsync(() ->
                delegate.checkRateLimit(purpose, phoneNumber, clientIp), rlExecutor)
        );
    }

    @PreDestroy
    public void shutdown() {
        rlExecutor.shutdown();
    }
}
