package dev.jotaeme.branchhub.application.demo.model;

/**
 * Public demo credentials and tool links shown on the login page. {@code grafanaUrl} is an empty
 * string when Grafana is not public.
 */
public record DemoAccess(String email, String password, String grafanaUrl) {
    public DemoAccess {
        grafanaUrl = grafanaUrl == null ? "" : grafanaUrl;
    }
}
