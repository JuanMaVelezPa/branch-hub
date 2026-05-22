package com.jm.spring_web.infrastructure.persistence.iam;

import com.jm.spring_web.application.security.port.RefreshSessionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(
        prefix = "security.refresh-session.cleanup",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RefreshSessionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshSessionCleanupScheduler.class);

    private final RefreshSessionPort refreshSessionPort;

    public RefreshSessionCleanupScheduler(RefreshSessionPort refreshSessionPort) {
        this.refreshSessionPort = refreshSessionPort;
    }

    @Scheduled(cron = "${security.refresh-session.cleanup.cron:0 0 * * * *}")
    public void purgeExpired() {
        long deleted = refreshSessionPort.purgeExpiredBefore(Instant.now());
        if (deleted > 0) {
            log.info("Purged {} expired iam_refresh_session row(s)", deleted);
        }
    }
}
