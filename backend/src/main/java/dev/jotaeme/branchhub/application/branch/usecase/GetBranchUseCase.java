package dev.jotaeme.branchhub.application.branch.usecase;

import dev.jotaeme.branchhub.application.branch.model.BranchResult;
import dev.jotaeme.branchhub.application.branch.port.BranchRepositoryPort;
import dev.jotaeme.branchhub.application.common.exception.NotFoundException;

import java.util.Objects;
import java.util.UUID;

public class GetBranchUseCase {
    private final BranchRepositoryPort branchRepositoryPort;

    public GetBranchUseCase(BranchRepositoryPort branchRepositoryPort) {
        this.branchRepositoryPort = Objects.requireNonNull(branchRepositoryPort);
    }

    public BranchResult execute(UUID branchId) {
        return branchRepositoryPort.findById(branchId)
                .map(BranchMapper::toResult)
                .orElseThrow(() -> new NotFoundException("Branch not found"));
    }
}
