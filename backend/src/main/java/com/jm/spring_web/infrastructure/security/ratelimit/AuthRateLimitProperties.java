package com.jm.spring_web.infrastructure.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.auth-rate-limit")
public record AuthRateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("false") boolean redisEnabled,
        @DefaultValue("auth:rl:") String redisKeyPrefix,
        @DefaultValue("30") int loginMaxRequests,
        @DefaultValue("PT1M") Duration loginWindow,
        @DefaultValue("30") int loginIdentifierMaxRequests,
        @DefaultValue("PT1M") Duration loginIdentifierWindow,
        @DefaultValue("120") int refreshMaxRequests,
        @DefaultValue("PT1M") Duration refreshWindow,
        @DefaultValue("50000") long cacheMaxEntries,
        @DefaultValue("PT10M") Duration cacheExpireAfterAccess) {
}
