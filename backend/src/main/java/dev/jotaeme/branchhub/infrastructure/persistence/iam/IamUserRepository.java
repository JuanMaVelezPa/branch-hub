package dev.jotaeme.branchhub.infrastructure.persistence.iam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IamUserRepository extends JpaRepository<IamUserJpaEntity, UUID> {
    Optional<IamUserJpaEntity> findByEmailIgnoreCase(String email);
}

