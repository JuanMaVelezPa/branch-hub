package dev.jotaeme.branchhub.infrastructure.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.scheduling.support.CronExpression;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoPropertiesTest {

    private static final DemoProperties.Reset RESET_OFF = new DemoProperties.Reset(false, "0 0 10 * * SAT", "America/Bogota");

    @Test
    void disabledDemoNeedsNoCredentials() {
        var props = new DemoProperties(false, new DemoProperties.User("", ""), null, RESET_OFF);

        assertThat(props.grafanaUrl()).isEmpty();
        assertThat(props.isDemoUser("")).isFalse();
    }

    @Test
    void enabledDemoWithoutEmailFailsNamingTheVariable() {
        assertThatThrownBy(() -> new DemoProperties(true, new DemoProperties.User(" ", "secret"), "", RESET_OFF))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_USER_EMAIL");
    }

    @Test
    void enabledDemoWithoutPasswordFailsNamingTheVariable() {
        assertThatThrownBy(() -> new DemoProperties(true, new DemoProperties.User("demo@x.dev", ""), "", RESET_OFF))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_USER_PASSWORD");
    }

    @Test
    void resetRequiresDemoEnabled() {
        var resetOn = new DemoProperties.Reset(true, "0 0 10 * * SAT", "America/Bogota");

        assertThatThrownBy(() -> new DemoProperties(false, new DemoProperties.User("", ""), "", resetOn))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_ENABLED");
    }

    @Test
    void isDemoUserMatchesTrimmedCaseInsensitiveEmailOnlyWhenEnabled() {
        var on = new DemoProperties(true, new DemoProperties.User(" Demo@BranchHub.dev ", "secret"), "", RESET_OFF);
        var off = new DemoProperties(false, new DemoProperties.User("demo@branchhub.dev", "secret"), "", RESET_OFF);

        assertThat(on.isDemoUser("  demo@branchhub.DEV")).isTrue();
        assertThat(on.isDemoUser("admin")).isFalse();
        assertThat(on.isDemoUser(null)).isFalse();
        assertThat(off.isDemoUser("demo@branchhub.dev")).isFalse();
    }

    @Test
    void defaultsAreOffAndTheResetIsWeeklyOnSaturdayAtTenColombiaTime() {
        DemoProperties defaults = new Binder(new MapConfigurationPropertySource(Map.of()))
                .bindOrCreate("app.demo", DemoProperties.class);

        assertThat(defaults.enabled()).isFalse();
        assertThat(defaults.reset().enabled()).isFalse();
        assertThat(defaults.reset().cron()).isEqualTo("0 0 10 * * SAT");
        assertThat(defaults.reset().zone()).isEqualTo("America/Bogota");

        ZoneId zone = ZoneId.of(defaults.reset().zone());
        ZonedDateTime wednesday = ZonedDateTime.of(2026, 11, 25, 12, 0, 0, 0, zone);
        ZonedDateTime next = CronExpression.parse(defaults.reset().cron()).next(wednesday);
        assertThat(next.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(next.toLocalDateTime()).isEqualTo(java.time.LocalDateTime.of(2026, 11, 28, 10, 0));
        assertThat(CronExpression.parse(defaults.reset().cron()).next(next))
                .isEqualTo(next.plusWeeks(1));
    }
}
