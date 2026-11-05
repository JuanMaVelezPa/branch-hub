package dev.jotaeme.branchhub.application.branch.usecase;

import dev.jotaeme.branchhub.application.branch.model.CreateBranchCommand;
import dev.jotaeme.branchhub.application.branch.port.BranchRepositoryPort;
import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.exception.ConflictException;
import dev.jotaeme.branchhub.application.common.pagination.PageSlice;
import dev.jotaeme.branchhub.application.notification.model.OutboxEventCommand;
import dev.jotaeme.branchhub.application.notification.port.OutboxEventRepositoryPort;
import dev.jotaeme.branchhub.domain.branch.Branch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateBranchUseCaseTest {

    @Test
    void shouldCreateBranchWhenCodeDoesNotExist() {
        InMemoryBranchRepository repository = new InMemoryBranchRepository();
        InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
        CreateBranchUseCase useCase = new CreateBranchUseCase(repository, outboxRepository);

        var result = useCase.execute(new CreateBranchCommand("BR001", "Main Branch", "Madrid"));

        assertEquals("BR001", result.code());
        assertEquals("Main Branch", result.name());
        assertEquals("Madrid", result.city());
        assertEquals("BRANCH_CREATED", outboxRepository.lastSavedCommand.eventType());
    }

    @Test
    void shouldFailWhenCodeAlreadyExists() {
        InMemoryBranchRepository repository = new InMemoryBranchRepository();
        InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
        repository.save(Branch.createNew("BR001", "Existing", "Madrid"));
        CreateBranchUseCase useCase = new CreateBranchUseCase(repository, outboxRepository);

        assertThrows(ConflictException.class, () -> useCase.execute(new CreateBranchCommand("BR001", "New", "Sevilla")));
    }

    @Test
    void shouldTrimCodeBeforeUniquenessCheck() {
        InMemoryBranchRepository repository = new InMemoryBranchRepository();
        InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
        repository.save(Branch.createNew("BR001", "Existing", "Madrid"));
        CreateBranchUseCase useCase = new CreateBranchUseCase(repository, outboxRepository);

        assertThrows(ConflictException.class, () -> useCase.execute(new CreateBranchCommand(" BR001 ", "New", "Sevilla")));
        assertTrue(repository.lastCheckedCode.equals("BR001"));
    }

    private static final class InMemoryOutboxRepository implements OutboxEventRepositoryPort {
        private OutboxEventCommand lastSavedCommand;

        @Override
        public void savePending(OutboxEventCommand command) {
            this.lastSavedCommand = command;
        }
    }

    private static final class InMemoryBranchRepository implements BranchRepositoryPort {
        private Branch branch;
        private String lastCheckedCode;

        @Override
        public boolean existsByCode(String code) {
            this.lastCheckedCode = code;
            return branch != null && branch.code().equals(code);
        }

        @Override
        public Branch save(Branch branch) {
            this.branch = branch;
            return branch;
        }

        @Override
        public Optional<Branch> findById(UUID id) {
            return Optional.ofNullable(branch);
        }

        @Override
        public PageSlice<Branch> findAll(PageQuery query) {
            if (branch == null) {
                return new PageSlice<>(List.of(), 0);
            }
            if (query.page() == 0) {
                return new PageSlice<>(List.of(branch), 1);
            }
            return new PageSlice<>(List.of(), 1);
        }
    }
}
