package dev.jotaeme.branchhub.application.demo.usecase;

import dev.jotaeme.branchhub.application.demo.model.DemoBranch;
import dev.jotaeme.branchhub.application.demo.port.DemoDataPort;
import dev.jotaeme.branchhub.application.demo.port.DemoSampleDataPort;

import java.util.List;
import java.util.OptionalInt;

/**
 * Puts the public demo back to its known state: sample branches only, empty outbox, and the demo
 * user restored.
 *
 * <p>All steps must run in one transaction. The boundary is set where the bean is wired, so this
 * class stays free of framework types.
 */
public class ResetDemoDataUseCase {
    private final DemoDataPort demoData;
    private final DemoSampleDataPort sampleData;
    private final String demoEmail;
    private final String demoPassword;

    public ResetDemoDataUseCase(
            DemoDataPort demoData,
            DemoSampleDataPort sampleData,
            String demoEmail,
            String demoPassword) {
        if (demoEmail == null || demoEmail.isBlank() || demoPassword == null || demoPassword.isBlank()) {
            throw new IllegalArgumentException("The demo reset needs the demo user email and password");
        }
        this.demoData = demoData;
        this.sampleData = sampleData;
        this.demoEmail = demoEmail;
        this.demoPassword = demoPassword;
    }

    /**
     * @return how many sample branches were inserted
     */
    public int execute() {
        List<DemoBranch> branches = sampleData.sampleBranches();
        demoData.resetBranches(branches);
        demoData.restoreDemoUser(demoEmail, demoPassword);
        return branches.size();
    }

    /**
     * Runs the reset only when the branch table is empty, so a fresh database starts with sample
     * data and a redeploy keeps the day's changes.
     *
     * @return how many sample branches were inserted, or empty when the table was not empty
     */
    public OptionalInt seedIfEmpty() {
        if (!demoData.isBranchTableEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(execute());
    }
}
