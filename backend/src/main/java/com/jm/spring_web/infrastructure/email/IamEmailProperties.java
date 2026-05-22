package com.jm.spring_web.infrastructure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.iam.email")
public record IamEmailProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("noreply@localhost") String fromAddress,
        @DefaultValue("5000") long relayDelayMs,
        @DefaultValue("50") int batchSize,
        @DefaultValue("5") int maxAttempts) {
}
