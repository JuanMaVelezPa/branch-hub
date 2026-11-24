package dev.jotaeme.branchhub.application.demo.port;

import dev.jotaeme.branchhub.application.demo.model.DemoBranch;

import java.util.List;

/**
 * Maintenance access to the demo data. Not a domain path: it writes rows directly and publishes no
 * events.
 */
public interface DemoDataPort {

    /**
     * Deletes every {@code processed_event}, {@code outbox_event} and {@code branch} row, then
     * inserts {@code branches}.
     */
    void resetBranches(List<DemoBranch> branches);

    /**
     * Makes the demo user enabled, unlocked, with {@code rawPassword} and exactly the APP_ADMIN
     * role; recreates it if missing.
     */
    void restoreDemoUser(String email, String rawPassword);

    boolean isBranchTableEmpty();
}
