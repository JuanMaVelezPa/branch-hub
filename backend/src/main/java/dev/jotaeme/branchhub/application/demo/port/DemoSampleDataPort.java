package dev.jotaeme.branchhub.application.demo.port;

import dev.jotaeme.branchhub.application.demo.model.DemoBranch;

import java.util.List;

/**
 * The sample branches the demo starts from and returns to on every reset.
 */
public interface DemoSampleDataPort {
    List<DemoBranch> sampleBranches();
}
