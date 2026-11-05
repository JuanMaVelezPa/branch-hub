package dev.jotaeme.branchhub.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for every test that starts a Spring context.
 *
 * <p>One PostgreSQL container is started for the whole Gradle test JVM and shared by every
 * subclass. It is started in a static initializer instead of through {@code @Testcontainers} and
 * {@code @Container}: JUnit would stop a {@code @Container} after each class, while Spring's
 * cached application context would still point to it. Ryuk removes the container when the JVM
 * exits.
 *
 * <p>{@code @ServiceConnection} gives Spring the datasource URL and credentials, so no test sets
 * {@code spring.datasource.*} itself.
 */
@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("branchhub")
            .withUsername("branchhub")
            .withPassword("branchhub");

    static {
        POSTGRES.start();
    }
}
