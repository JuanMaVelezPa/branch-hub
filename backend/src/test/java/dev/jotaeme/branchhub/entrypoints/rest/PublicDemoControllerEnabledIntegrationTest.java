package dev.jotaeme.branchhub.entrypoints.rest;

import dev.jotaeme.branchhub.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC 05 step 2: with the demo on, {@code GET /api/v1/public/demo} answers 200 without a token.
 */
@AutoConfigureMockMvc
class PublicDemoControllerEnabledIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String DEMO_EMAIL = "demo-endpoint@branchhub.test";
    private static final String DEMO_PASSWORD = "Demo_Endpoint_Pass_2026!";
    private static final String GRAFANA_URL = "https://grafana.example.test";

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.demo.enabled", () -> "true");
        registry.add("app.demo.user.email", () -> DEMO_EMAIL);
        registry.add("app.demo.user.password", () -> DEMO_PASSWORD);
        registry.add("app.demo.grafana-url", () -> GRAFANA_URL);
    }

    @Test
    void returnsDemoAccessWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/public/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(DEMO_EMAIL))
                .andExpect(jsonPath("$.password").value(DEMO_PASSWORD))
                .andExpect(jsonPath("$.grafanaUrl").value(GRAFANA_URL));
    }
}
