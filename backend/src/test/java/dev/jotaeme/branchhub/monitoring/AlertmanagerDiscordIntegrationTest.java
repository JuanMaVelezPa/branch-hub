package dev.jotaeme.branchhub.monitoring;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AlertmanagerDiscordIntegrationTest {

    @Test
    void shouldSendManualTestAlertToAlertmanagerWhenOptInEnabled() throws Exception {
        String enabled = System.getenv("RUN_ALERTMANAGER_TEST");
        assumeTrue("true".equalsIgnoreCase(enabled),
                "Set RUN_ALERTMANAGER_TEST=true to execute this integration test.");

        assumeTrue(isAlertmanagerAvailable(),
                "Alertmanager is not reachable at http://localhost:9093");

        String payload = """
                [
                  {
                    "labels": {
                      "alertname": "DiscordTestAlertFromJUnit",
                      "severity": "warning",
                      "source": "junit-test"
                    },
                    "annotations": {
                      "summary": "JUnit Discord alert test",
                      "description": "Manual test alert sent from JUnit to verify Alertmanager -> Discord webhook."
                    },
                    "startsAt": "2026-04-08T15:30:00Z",
                    "endsAt": "2030-04-08T15:40:00Z"
                  }
                ]
                """;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:9093/api/v2/alerts"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Alertmanager should accept test alert");
    }

    private boolean isAlertmanagerAvailable() {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create("http://localhost:9093/-/ready").toURL().openConnection();
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            connection.setRequestMethod("GET");
            return connection.getResponseCode() == 200;
        } catch (IOException exception) {
            return false;
        }
    }
}
