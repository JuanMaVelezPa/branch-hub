package dev.jotaeme.branchhub.application.demo.usecase;

import dev.jotaeme.branchhub.application.demo.model.DemoAccess;

import java.util.Optional;

/**
 * Returns the public demo access when the demo is on, or empty when it is off. The value comes from
 * configuration and is fixed for the life of the application.
 */
public class GetDemoAccessUseCase {
    private final Optional<DemoAccess> demoAccess;

    /**
     * @param demoAccess the demo access, or {@code null} when the demo is off
     */
    public GetDemoAccessUseCase(DemoAccess demoAccess) {
        this.demoAccess = Optional.ofNullable(demoAccess);
    }

    public Optional<DemoAccess> execute() {
        return demoAccess;
    }
}
