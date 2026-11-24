package dev.jotaeme.branchhub.entrypoints.rest.dto;

public record DemoAccessResponse(
        String email,
        String password,
        String grafanaUrl
) {
}
