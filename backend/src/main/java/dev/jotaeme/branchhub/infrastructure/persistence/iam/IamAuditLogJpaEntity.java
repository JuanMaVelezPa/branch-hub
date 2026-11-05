package dev.jotaeme.branchhub.infrastructure.persistence.iam;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iam_audit_log")
public class IamAuditLogJpaEntity {
    @Id
    private UUID id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IamAuditLogJpaEntity() {
    }

    public IamAuditLogJpaEntity(UUID id, UUID actorUserId, String action, UUID targetUserId, String metadata, Instant createdAt) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetUserId = targetUserId;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

