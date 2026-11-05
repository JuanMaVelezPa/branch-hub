package dev.jotaeme.branchhub.infrastructure.security;

import dev.jotaeme.branchhub.application.security.port.IamAuditPort;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamAuditLogJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamAuditLogRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class JpaIamAuditAdapter implements IamAuditPort {
    private final IamAuditLogRepository repository;

    public JpaIamAuditAdapter(IamAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(UUID actorUserId, String action, UUID targetUserId, String metadata) {
        repository.save(new IamAuditLogJpaEntity(
                UUID.randomUUID(),
                actorUserId,
                action,
                targetUserId,
                metadata,
                Instant.now()
        ));
    }
}

