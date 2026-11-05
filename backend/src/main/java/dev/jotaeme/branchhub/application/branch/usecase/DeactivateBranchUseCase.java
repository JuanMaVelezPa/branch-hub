package dev.jotaeme.branchhub.application.branch.usecase;

import dev.jotaeme.branchhub.application.branch.model.BranchResult;
import dev.jotaeme.branchhub.application.branch.port.BranchRepositoryPort;
import dev.jotaeme.branchhub.application.common.exception.NotFoundException;
import dev.jotaeme.branchhub.domain.branch.Branch;

import java.util.Objects;
import java.util.UUID;

public class DeactivateBranchUseCase {
    private final BranchRepositoryPort branchRepositoryPort;

    public DeactivateBranchUseCase(BranchRepositoryPort branchRepositoryPort) {
        this.branchRepositoryPort = Objects.requireNonNull(branchRepositoryPort);
    }

    public BranchResult execute(UUID branchId) {
        Branch existing = branchRepositoryPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));

        Branch saved = branchRepositoryPort.save(existing.deactivate());
        return BranchMapper.toResult(saved);
    }
}
