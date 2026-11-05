package dev.jotaeme.branchhub.application.branch.usecase;

import dev.jotaeme.branchhub.application.branch.model.BranchResult;
import dev.jotaeme.branchhub.application.branch.port.BranchRepositoryPort;
import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.application.common.pagination.PageSlice;
import dev.jotaeme.branchhub.domain.branch.Branch;

import java.util.Objects;

public class ListBranchesUseCase {
    private final BranchRepositoryPort branchRepositoryPort;

    public ListBranchesUseCase(BranchRepositoryPort branchRepositoryPort) {
        this.branchRepositoryPort = Objects.requireNonNull(branchRepositoryPort);
    }

    public PageResult<BranchResult> execute(PageQuery query) {
        PageSlice<Branch> slice = branchRepositoryPort.findAll(query);
        var content = slice.content().stream().map(BranchMapper::toResult).toList();
        return PageResult.of(content, slice.totalElements(), query.page(), query.size());
    }
}
