package com.jm.spring_web.infrastructure.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(
        prefix = "security.auth-rate-limit",
        name = "redis-enabled",
        havingValue = "true")
public class RedisAuthRateLimiter implements AuthRateLimiter {

    private final AuthRateLimitProperties properties;
    private final ProxyManager<String> proxyManager;
    private final String keyPrefix;

    public RedisAuthRateLimiter(
            AuthRateLimitProperties properties,
            ProxyManager<String> authRateLimitProxyManager) {
        this.properties = properties;
        this.proxyManager = authRateLimitProxyManager;
        this.keyPrefix = properties.redisKeyPrefix();
    }

    @Override
    public boolean tryLogin(String clientIp, Optional<String> normalizedLoginIdentifier) {
        if (!properties.enabled()) {
            return true;
        }
        String ipKey = keyPrefix + "login:ip:" + clientIp;
        if (!proxyManager.getProxy(ipKey, loginConfigSupplier()).tryConsume(1)) {
            return false;
        }
        if (normalizedLoginIdentifier.isEmpty()) {
            return true;
        }
        String userKey = keyPrefix + "login:user:" + normalizedLoginIdentifier.get();
        return proxyManager.getProxy(userKey, loginIdentifierConfigSupplier()).tryConsume(1);
    }

    @Override
    public boolean tryRefresh(String clientIp) {
        if (!properties.enabled()) {
            return true;
        }
        String key = keyPrefix + "refresh:" + clientIp;
        return proxyManager.getProxy(key, refreshConfigSupplier()).tryConsume(1);
    }

    private Supplier<BucketConfiguration> loginConfigSupplier() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.loginMaxRequests())
                        .refillIntervally(properties.loginMaxRequests(), properties.loginWindow())
                        .build())
                .build();
    }

    private Supplier<BucketConfiguration> loginIdentifierConfigSupplier() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.loginIdentifierMaxRequests())
                        .refillIntervally(
                                properties.loginIdentifierMaxRequests(), properties.loginIdentifierWindow())
                        .build())
                .build();
    }

    private Supplier<BucketConfiguration> refreshConfigSupplier() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.refreshMaxRequests())
                        .refillIntervally(properties.refreshMaxRequests(), properties.refreshWindow())
                        .build())
                .build();
    }
}
