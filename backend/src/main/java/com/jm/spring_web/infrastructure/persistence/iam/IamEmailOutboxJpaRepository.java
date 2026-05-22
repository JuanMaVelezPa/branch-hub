package com.jm.spring_web.infrastructure.persistence.iam;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IamEmailOutboxJpaRepository extends JpaRepository<IamEmailOutboxJpaEntity, UUID> {

    List<IamEmailOutboxJpaEntity> findAllByStatusOrderByCreatedAtAsc(IamEmailOutboxStatus status, Pageable pageable);
}
