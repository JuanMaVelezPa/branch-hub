package dev.jotaeme.branchhub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataBranchRepository extends JpaRepository<BranchJpaEntity, UUID> {
    boolean existsByCode(String code);
}
