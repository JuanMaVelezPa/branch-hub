package dev.jotaeme.branchhub.application.branch.port;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageSlice;

import java.util.Optional;
import java.util.UUID;

import dev.jotaeme.branchhub.domain.branch.Branch;

public interface BranchRepositoryPort {
    boolean existsByCode(String code);

    Branch save(Branch branch);

    Optional<Branch> findById(UUID id);

    PageSlice<Branch> findAll(PageQuery query);
}
