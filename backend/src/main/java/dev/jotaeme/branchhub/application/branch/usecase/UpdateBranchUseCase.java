package dev.jotaeme.branchhub.application.branch.usecase;

import dev.jotaeme.branchhub.application.branch.model.BranchResult;
import dev.jotaeme.branchhub.application.branch.model.UpdateBranchCommand;
import dev.jotaeme.branchhub.application.branch.port.BranchRepositoryPort;
import dev.jotaeme.branchhub.application.common.exception.NotFoundException;
import dev.jotaeme.branchhub.application.common.exception.UnprocessableEntityException;
import dev.jotaeme.branchhub.domain.branch.Branch;

import java.util.Objects;
import java.util.UUID;

public class UpdateBranchUseCase {
    private final BranchRepositoryPort branchRepositoryPort;

    public UpdateBranchUseCase(BranchRepositoryPort branchRepositoryPort) {
        this.branchRepositoryPort = Objects.requireNonNull(branchRepositoryPort);
    }

    public BranchResult execute(UUID branchId, UpdateBranchCommand command) {
        Branch existing = branchRepositoryPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));

        if (!existing.active()) {
            throw new UnprocessableEntityException("Inactive branches cannot be updated");
        }

        Branch saved = branchRepositoryPort.save(existing.update(command.name(), command.city()));
        return BranchMapper.toResult(saved);
    }
}
