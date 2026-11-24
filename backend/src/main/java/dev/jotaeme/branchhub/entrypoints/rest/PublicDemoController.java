package dev.jotaeme.branchhub.entrypoints.rest;

import dev.jotaeme.branchhub.application.common.exception.NotFoundException;
import dev.jotaeme.branchhub.application.demo.usecase.GetDemoAccessUseCase;
import dev.jotaeme.branchhub.entrypoints.rest.dto.DemoAccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (no auth) endpoint that tells the login page whether the demo is on and with which
 * credentials. Returns 404 Problem Details when the demo is off.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicDemoController {
    private final GetDemoAccessUseCase getDemoAccessUseCase;

    public PublicDemoController(GetDemoAccessUseCase getDemoAccessUseCase) {
        this.getDemoAccessUseCase = getDemoAccessUseCase;
    }

    @GetMapping("/demo")
    public DemoAccessResponse demo() {
        return getDemoAccessUseCase.execute()
                .map(access -> new DemoAccessResponse(access.email(), access.password(), access.grafanaUrl()))
                .orElseThrow(() -> new NotFoundException("The public demo is not enabled"));
    }
}
