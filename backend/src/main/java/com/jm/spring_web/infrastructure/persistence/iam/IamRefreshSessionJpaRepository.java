package com.jm.spring_web.infrastructure.persistence.iam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IamRefreshSessionJpaRepository extends JpaRepository<IamRefreshSessionJpaEntity, UUID> {

    Optional<IamRefreshSessionJpaEntity> findByTokenHash(String tokenHash);

    List<IamRefreshSessionJpaEntity> findAllByFamilyId(UUID familyId);

    List<IamRefreshSessionJpaEntity> findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId, Instant now);

    Optional<IamRefreshSessionJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true)
    @Query(
            "update IamRefreshSessionJpaEntity s set s.revokedAt = :now where s.userId = :userId"
                    + " and s.familyId <> :keepFamilyId and s.revokedAt is null")
    int revokeActiveInOtherFamilies(
            @Param("userId") UUID userId,
            @Param("keepFamilyId") UUID keepFamilyId,
            @Param("now") Instant now);

    long deleteByExpiresAtBefore(Instant expiresAt);
}
