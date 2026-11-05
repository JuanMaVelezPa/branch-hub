package dev.jotaeme.branchhub.application.branch.usecase;

import dev.jotaeme.branchhub.application.branch.BranchListPagination;
import dev.jotaeme.branchhub.application.branch.port.BranchRepositoryPort;
import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageSlice;
import dev.jotaeme.branchhub.domain.branch.Branch;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListBranchesUseCaseTest {

    @Test
    void shouldReturnMappedPage() {
        BranchRepositoryPort repository = Mockito.mock(BranchRepositoryPort.class);
        Mockito.when(repository.findAll(ArgumentMatchers.any(PageQuery.class))).thenReturn(new PageSlice<>(List.of(
                Branch.createNew("BR001", "Main", "Madrid"),
                Branch.createNew("BR002", "North", "Bilbao")
        ), 2));
        ListBranchesUseCase useCase = new ListBranchesUseCase(repository);

        var result = useCase.execute(
                new PageQuery(0, 20, BranchListPagination.SORT_POLICY.defaultSort()));

        assertEquals(2, result.content().size());
        assertEquals(2, result.totalElements());
        assertEquals(0, result.page());
        assertEquals(20, result.size());
        assertEquals(1, result.totalPages());
        assertEquals("BR001", result.content().get(0).code());
        assertEquals("BR002", result.content().get(1).code());
    }

    @Test
    void shouldReturnEmptyPageWhenNoBranchesExist() {
        BranchRepositoryPort repository = Mockito.mock(BranchRepositoryPort.class);
        Mockito.when(repository.findAll(ArgumentMatchers.any(PageQuery.class))).thenReturn(new PageSlice<>(List.of(), 0));
        ListBranchesUseCase useCase = new ListBranchesUseCase(repository);

        var result = useCase.execute(
                new PageQuery(0, 20, BranchListPagination.SORT_POLICY.defaultSort()));

        assertEquals(0, result.content().size());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void shouldComputeTotalPagesAcrossSeveralPages() {
        BranchRepositoryPort repository = Mockito.mock(BranchRepositoryPort.class);
        Mockito.when(repository.findAll(ArgumentMatchers.any(PageQuery.class))).thenReturn(new PageSlice<>(List.of(
                Branch.createNew("BR001", "Main", "Madrid"),
                Branch.createNew("BR002", "North", "Bilbao")
        ), 5));
        ListBranchesUseCase useCase = new ListBranchesUseCase(repository);

        var result = useCase.execute(
                new PageQuery(0, 2, BranchListPagination.SORT_POLICY.defaultSort()));

        assertEquals(3, result.totalPages());
    }
}
