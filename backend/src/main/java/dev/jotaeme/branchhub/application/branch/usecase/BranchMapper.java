package dev.jotaeme.branchhub.application.branch.usecase;

import dev.jotaeme.branchhub.application.branch.model.BranchResult;
import dev.jotaeme.branchhub.domain.branch.Branch;

final class BranchMapper {
    private BranchMapper() {
    }

    static BranchResult toResult(Branch branch) {
        return new BranchResult(
                branch.id(),
                branch.code(),
                branch.name(),
                branch.city(),
                branch.active(),
                branch.createdAt(),
                branch.updatedAt());
    }
}
