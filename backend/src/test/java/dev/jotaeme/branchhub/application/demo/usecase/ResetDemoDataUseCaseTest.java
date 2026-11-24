package dev.jotaeme.branchhub.application.demo.usecase;

import dev.jotaeme.branchhub.application.demo.model.DemoBranch;
import dev.jotaeme.branchhub.application.demo.port.DemoDataPort;
import dev.jotaeme.branchhub.application.demo.port.DemoSampleDataPort;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResetDemoDataUseCaseTest {

    private final DemoDataPort demoData = mock(DemoDataPort.class);
    private final DemoSampleDataPort sampleData = mock(DemoSampleDataPort.class);
    private final List<DemoBranch> sample = List.of(
            new DemoBranch("BR-001", "One", "Bogotá", true),
            new DemoBranch("BR-002", "Two", "Cali", false));

    @Test
    void resetsBranchesThenRestoresTheDemoUser() {
        when(sampleData.sampleBranches()).thenReturn(sample);
        var useCase = new ResetDemoDataUseCase(demoData, sampleData, "demo@x.dev", "secret");

        assertThat(useCase.execute()).isEqualTo(2);

        InOrder order = inOrder(demoData);
        order.verify(demoData).resetBranches(sample);
        order.verify(demoData).restoreDemoUser("demo@x.dev", "secret");
    }

    @Test
    void seedIfEmptyRunsOnlyOnAnEmptyBranchTable() {
        when(sampleData.sampleBranches()).thenReturn(sample);
        var useCase = new ResetDemoDataUseCase(demoData, sampleData, "demo@x.dev", "secret");

        when(demoData.isBranchTableEmpty()).thenReturn(false);
        assertThat(useCase.seedIfEmpty()).isEmpty();
        verify(demoData, never()).resetBranches(sample);

        when(demoData.isBranchTableEmpty()).thenReturn(true);
        assertThat(useCase.seedIfEmpty()).hasValue(2);
        verify(demoData).resetBranches(sample);
    }

    @Test
    void rejectsMissingCredentials() {
        assertThatThrownBy(() -> new ResetDemoDataUseCase(demoData, sampleData, " ", "secret"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResetDemoDataUseCase(demoData, sampleData, "demo@x.dev", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
