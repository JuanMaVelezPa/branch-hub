package dev.jotaeme.branchhub.infrastructure.persistence.iam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IamRoleRepository extends JpaRepository<IamRoleJpaEntity, UUID> {
    Optional<IamRoleJpaEntity> findByName(String name);
}

