package dev.jotaeme.branchhub.application.demo.usecase;

import dev.jotaeme.branchhub.application.demo.model.DemoAccess;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GetDemoAccessUseCaseTest {

    @Test
    void returnsAccessWhenConfigured() {
        var access = new DemoAccess("demo@x.dev", "secret", null);

        var result = new GetDemoAccessUseCase(access).execute();

        assertThat(result).contains(new DemoAccess("demo@x.dev", "secret", ""));
    }

    @Test
    void returnsEmptyWhenDemoIsOff() {
        assertThat(new GetDemoAccessUseCase(null).execute()).isEmpty();
    }
}
