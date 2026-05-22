package com.jm.spring_web.infrastructure.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(
        prefix = "security.auth-rate-limit",
        name = "redis-enabled",
        havingValue = "false",
        matchIfMissing = true)
public class CaffeineAuthRateLimiter implements AuthRateLimiter {

    private final AuthRateLimitProperties properties;
    private final Cache<String, Bucket> loginIpBuckets;
    private final Cache<String, Bucket> loginIdentifierBuckets;
    private final Cache<String, Bucket> refreshBuckets;

    public CaffeineAuthRateLimiter(AuthRateLimitProperties properties) {
        this.properties = properties;
        this.loginIpBuckets = buildCache(properties.cacheMaxEntries(), properties.cacheExpireAfterAccess());
        this.loginIdentifierBuckets = buildCache(properties.cacheMaxEntries(), properties.cacheExpireAfterAccess());
        this.refreshBuckets = buildCache(properties.cacheMaxEntries(), properties.cacheExpireAfterAccess());
    }

    @Override
    public boolean tryLogin(String clientIp, Optional<String> normalizedLoginIdentifier) {
        if (!properties.enabled()) {
            return true;
        }
        if (!loginIpBuckets.get(clientIp, k -> newLoginIpBucket()).tryConsume(1)) {
            return false;
        }
        if (normalizedLoginIdentifier.isEmpty()) {
            return true;
        }
        return loginIdentifierBuckets
                .get(normalizedLoginIdentifier.get(), k -> newLoginIdentifierBucket())
                .tryConsume(1);
    }

    @Override
    public boolean tryRefresh(String clientIp) {
        if (!properties.enabled()) {
            return true;
        }
        return refreshBuckets.get(clientIp, k -> newRefreshBucket()).tryConsume(1);
    }

    private Bucket newLoginIpBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.loginMaxRequests())
                        .refillIntervally(properties.loginMaxRequests(), properties.loginWindow())
                        .build())
                .build();
    }

    private Bucket newLoginIdentifierBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.loginIdentifierMaxRequests())
                        .refillIntervally(
                                properties.loginIdentifierMaxRequests(), properties.loginIdentifierWindow())
                        .build())
                .build();
    }

    private Bucket newRefreshBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.refreshMaxRequests())
                        .refillIntervally(properties.refreshMaxRequests(), properties.refreshWindow())
                        .build())
                .build();
    }

    private static Cache<String, Bucket> buildCache(long maxEntries, java.time.Duration expireAfterAccess) {
        return Caffeine.newBuilder()
                .maximumSize(maxEntries)
                .expireAfterAccess(expireAfterAccess)
                .build();
    }
}
