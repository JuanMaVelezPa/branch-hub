package dev.jotaeme.branchhub.infrastructure.persistence.iam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IamAuditLogRepository extends JpaRepository<IamAuditLogJpaEntity, UUID> {
}

