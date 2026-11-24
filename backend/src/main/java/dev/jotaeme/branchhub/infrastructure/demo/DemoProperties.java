package dev.jotaeme.branchhub.infrastructure.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Public demo settings ({@code app.demo.*}). Everything is off by default; the prod overlay turns
 * it on from {@code .env}.
 *
 * <p>Validated when bound, so a misconfigured demo fails startup with a message that names the
 * missing variable.
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue User user,
        @DefaultValue("") String grafanaUrl,
        @DefaultValue Reset reset) {

    /** The only role the demo user may have. Its credentials are public, so it must never get more. */
    public static final String DEMO_ROLE = "APP_ADMIN";

    public DemoProperties {
        grafanaUrl = grafanaUrl == null ? "" : grafanaUrl.trim();
        if (enabled) {
            if (isBlank(user.email())) {
                throw new IllegalStateException(
                        "app.demo.user.email (DEMO_USER_EMAIL) must be set when app.demo.enabled=true");
            }
            if (isBlank(user.password())) {
                throw new IllegalStateException(
                        "app.demo.user.password (DEMO_USER_PASSWORD) must be set when app.demo.enabled=true");
            }
        }
        if (reset.enabled() && !enabled) {
            throw new IllegalStateException(
                    "app.demo.reset.enabled (DEMO_RESET_ENABLED) requires app.demo.enabled=true (DEMO_ENABLED)");
        }
    }

    /**
     * Whether {@code email} is the demo account while the demo is on (case-insensitive, trimmed).
     */
    public boolean isDemoUser(String email) {
        return enabled && email != null && email.trim().equalsIgnoreCase(user.email());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record User(@DefaultValue("") String email, @DefaultValue("") String password) {
        public User {
            email = email == null ? "" : email.trim();
            password = password == null ? "" : password;
        }
    }

    public record Reset(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("0 0 10 * * SAT") String cron,
            @DefaultValue("America/Bogota") String zone) {
    }
}
