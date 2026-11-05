package dev.jotaeme.branchhub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
    List<OutboxEventJpaEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
