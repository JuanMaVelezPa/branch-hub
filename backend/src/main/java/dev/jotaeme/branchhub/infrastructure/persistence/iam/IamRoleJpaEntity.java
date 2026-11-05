package dev.jotaeme.branchhub.infrastructure.persistence.iam;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iam_role")
public class IamRoleJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IamRoleJpaEntity() {
    }

    public IamRoleJpaEntity(UUID id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

