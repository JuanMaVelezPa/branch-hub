package dev.jotaeme.branchhub.application.branch.usecase;

import dev.jotaeme.branchhub.application.branch.model.UpdateBranchCommand;
import dev.jotaeme.branchhub.application.branch.port.BranchRepositoryPort;
import dev.jotaeme.branchhub.application.common.exception.NotFoundException;
import dev.jotaeme.branchhub.application.common.exception.UnprocessableEntityException;
import dev.jotaeme.branchhub.domain.branch.Branch;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateBranchUseCaseTest {

    @Test
    void shouldUpdateBranchWhenActive() {
        BranchRepositoryPort repository = Mockito.mock(BranchRepositoryPort.class);
        UUID id = UUID.randomUUID();
        Branch existing = Branch.restore(id, "BR001", "Main", "Madrid", true, LocalDateTime.now(), LocalDateTime.now());
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(existing));
        Mockito.when(repository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateBranchUseCase useCase = new UpdateBranchUseCase(repository);

        var result = useCase.execute(id, new UpdateBranchCommand("Main Updated", "Sevilla"));

        assertEquals("Main Updated", result.name());
        assertEquals("Sevilla", result.city());
        assertEquals("BR001", result.code());
    }

    @Test
    void shouldThrowNotFoundWhenBranchDoesNotExist() {
        BranchRepositoryPort repository = Mockito.mock(BranchRepositoryPort.class);
        UUID id = UUID.randomUUID();
        Mockito.when(repository.findById(id)).thenReturn(Optional.empty());
        UpdateBranchUseCase useCase = new UpdateBranchUseCase(repository);

        assertThrows(NotFoundException.class, () -> useCase.execute(id, new UpdateBranchCommand("Any", "Any")));
    }

    @Test
    void shouldThrowUnprocessableWhenBranchIsInactive() {
        BranchRepositoryPort repository = Mockito.mock(BranchRepositoryPort.class);
        UUID id = UUID.randomUUID();
        Branch inactive = Branch.restore(id, "BR001", "Main", "Madrid", false, LocalDateTime.now(), LocalDateTime.now());
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(inactive));
        UpdateBranchUseCase useCase = new UpdateBranchUseCase(repository);

        assertThrows(UnprocessableEntityException.class, () -> useCase.execute(id, new UpdateBranchCommand("Any", "Any")));
    }
}
