# Backend — Branch Hub API

Spring Boot **4** service (Java **25**, Gradle) for Branch Hub. Clean Architecture under `dev.jotaeme.branchhub`: `domain` ← `application` ← `infrastructure` / `entrypoints` (see [architecture](../docs/architecture.md)).

**Monorepo:** run Docker Compose and Git commands from the **repository root** unless noted. Gradle output lives in `backend/build/` (standard for this module).

## What this demonstrates

- JWT auth, refresh cookie, Problem Details (RFC 7807)
- Transactional outbox + Kafka, retries, DLT, idempotent consumer
- JSON logging + `transactionId`, Prometheus metrics, Grafana dashboards

## Tech stack

- Java 25, Spring Boot 4, Gradle
- PostgreSQL, Flyway, Spring Security
- Apache Kafka, Micrometer, Actuator

## Database migrations (Flyway)

Schema changes live **in this module** next to the code that uses them — same repo, same review, same CI. Configuration: `spring.flyway.locations=classpath:db/migration` in `application.properties`.

> **Known issue:** Flyway does not run yet. Spring Boot 4 needs `spring-boot-starter-flyway`, and `build.gradle` only declares `flyway-core`. In Docker Compose the `docker` profile creates the schema with Hibernate (`ddl-auto=update`), and the integration tests create their tables with SQL. The scripts below are still the intended source of truth; the fix is planned as the next spec.

| Location | Role |
|----------|------|
| `src/main/resources/db/migration/` | SQL scripts Flyway applies on startup (PostgreSQL). |

### Versioned migrations (`V__`)

- **Naming:** `V{version}__{short_description}.sql` — e.g. `V3__add_user_roles.sql`. Use a **single increasing version** per new file (integer or with underscores per [Flyway rules](https://documentation.red-gate.com/flyway)).
- **Executed once** per database; Flyway records them in `flyway_schema_history`.
- **Do not edit** a `V*` script that has already been applied in **shared** environments (dev/staging/prod). Fix forward with a **new** `V*` migration (`ALTER`, `DROP`, data backfill, etc.). Editing old files is only safe on **throwaway local DBs** before anyone else applies them.

### Repeatable migrations (`R__`)

- **Naming:** `R__{name}.sql` — re-run when the file **checksum** changes (e.g. redefine a `VIEW` or `FUNCTION`).
- Use for definitions that are **idempotent** and fully replaced each time, not for one-off data fixes.

### What to put in one file

- Prefer **one migration per coherent change** (feature or ticket): e.g. new tables + indexes + FKs needed together. Splitting “one table per file” is optional; splitting unrelated changes into one giant file hurts review.
- **Tables, indexes, constraints:** usual content of `V__` scripts.
- **Functions, triggers:** same folder; order matters — create tables in an earlier `V` than triggers that reference them. For frequently edited routines, consider `R__` if appropriate.

### PostgreSQL notes

- There are no Oracle-style **packages**; use **schemas** (`CREATE SCHEMA`) or clear naming under `public`.
- Prefer explicit **`IF NOT EXISTS`** only where Flyway semantics allow; rely on **versioned** scripts for deterministic history.

### Tests and local dev

- Target: tests that need a real schema use the same Flyway-managed schema (e.g. Testcontainers) so migrations stay the **single source of truth**. Today the integration tests create their tables with SQL (see the known issue above).

## Run with Docker (full stack)

From **repo root**:

```bash
cp .env.example .env
cp monitoring/alertmanager/alertmanager.local.yml.example monitoring/alertmanager/alertmanager.local.yml
cp monitoring/alertmanager/discord-webhook-url.example monitoring/alertmanager/discord-webhook-url
docker compose up -d --build
```

Configure `JWT_SECRET`, `APP_SUPER_ADMIN_PASSWORD`, and (optional) Discord webhook per root `.env.example`. Full guide, including the database URLs and troubleshooting: [docs/getting-started.md](../docs/getting-started.md).

**URLs (Docker Compose on the host, default ports; change them with `WEB_PORT` / `API_PORT` in `.env`):**

| Service | Host port | Notes |
|--------|-----------|--------|
| **`web`** (Nginx + Angular) | **8080** | Browser entry. Proxies `/api`, `/actuator`, `/swagger-ui`, `/v3/*` to `app` so the UI keeps a single origin. |
| **`app`** (Spring Boot) | **8081** | Direct API: Postman, `curl`, Swagger UI at http://localhost:8081/swagger-ui.html, Actuator, etc. |
| PostgreSQL | **5433** | Database `branchhub`, user `branchhub`. Exposed for local tooling (psql, migrations debugging). Inside the Compose network the hostname is `postgres:5432`. |
| Grafana | 3000 | Dashboard `branch-hub-observability` (folder **Branch Hub**). |
| Prometheus | 9090 | Job **`branch-hub-api`** scrapes **`app:8080`** on the Compose network (not via the host port). |

Separate images: **`app`** is built from `backend/` (Spring Boot only); **`web`** from `frontend/` (Angular production build + `nginx` image with `nginx.conf`).

## Production-like compose

From **repo root**:

```bash
cp .env.prod.example .env.prod
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

With `prod`: Swagger disabled, actuator reduced, health details hidden.

## Architecture

| Layer | Responsibility |
|-------|----------------|
| `domain` | Entities and core rules |
| `application` | Use cases, ports |
| `infrastructure` | DB, Kafka, security, observability |
| `entrypoints` | REST controllers |

**Branch flow:** controller → use case → persist `branch` + `outbox_event` → relay publishes to Kafka → consumer idempotent via `processed_event`.

## Security (learning index)

Phase-aligned notes (Problem Details, JWT + refresh cookie, what minification does *not* do, CSP, etc.): [**docs/security.md**](../docs/security.md).

## Security matrix (summary)

- Public: `/api/v1/auth/**`, Swagger/OpenAPI (non-prod), `GET /actuator/health`, `GET /actuator/info`
- Optional public: `/actuator/metrics/**`, `/actuator/prometheus` (configurable)
- **SUPER_ADMIN:** `/api/v1/admin/**` (users, roles, **audit log** `GET /api/v1/admin/audit-logs`)
- **SUPER_ADMIN / APP_ADMIN:** `/api/v1/branches/**`
- **Authenticated (any role):** `GET /api/v1/me` (current user profile: id, email, roles, …)
- Other routes: authenticated

Spring does **not** serve the static UI; the **`web`** container (Nginx) does on host port **8080**. The API is also reachable **directly** on host port **8081** for tools and clarity.

**Auth:** login sets HttpOnly refresh cookie; access token in response body (SPA keeps it in memory). `POST /api/v1/auth/logout` clears refresh cookie (`204`).

### IAM bootstrap (local)

On startup, the backend can seed a first **SUPER_ADMIN** account (if missing) so a fresh install is usable without manual SQL. The account is only created when no user with `APP_SUPER_ADMIN_EMAIL` exists.

- **Enable/disable:** `APP_IAM_BOOTSTRAP_ENABLED` (default `true`)
- **Seed email:** `APP_SUPER_ADMIN_EMAIL` (default `admin@example.com`)
- **Seed password:** `APP_SUPER_ADMIN_PASSWORD` (default `Admin_ChangeMe_2026!`)

For production, treat this as a **break-glass** path: rotate secrets and restrict access to env/config.

## Pagination (standard for list endpoints)

`GET /api/v1/branches?page=0&size=10` returns `content`, `totalElements`, `page`, `size`, `totalPages`. Defaults `page=0`, `size=10`, max `size=100`. Optional `sort=property,asc|desc` (allowed fields are per resource).

**Layers (reuse for new aggregates):**

| Piece | Role |
|-------|------|
| `PageRequestParams` | HTTP query (`page`, `size`, `sort`) with Bean Validation |
| `PaginationBinding` | Maps to application `PageQuery` + resource `SortPolicy` |
| `PageQuery` / `SortOrder` / `SortPolicy` | Framework-agnostic pagination in `application.common.pagination` |
| `PageSlice` → `PageResult` | Repository slice → use case result; `PageMapper` → `PagedResponse` |
| `SpringPageRequests` | `PageQuery` → Spring `PageRequest` (infrastructure only) |

Each new list defines a `SortPolicy` (default + allowed JPA names), e.g. `BranchListPagination.SORT_POLICY`. The SPA sends `sort` aligned with that contract.

## Problem Details (errors)

Handled errors use `application/problem+json`-style bodies (`type`, `title`, `status`, `detail`, `instance`, `timestamp`, optional `errors`).

## Metrics (examples)

`outbox_lag`, `failed_notifications_total`, `app_auth_login_total`, `app_branch_command_total`, `app_iam_admin_action_total`, `app_use_case_duration`, `app_outbox_publish_total`, `app_notification_consumer_total`.

## Alerting

Prometheus rules (group `branch-hub-alerts`) → Alertmanager → Discord (see `monitoring/`). Alerts include API down (`BranchHubApiDown`), restart loops (`BranchHubApiFlapping`), Kafka/Postgres, outbox lag, consumer DLT, etc.

## Tests

From **`backend/`**:

```bash
./gradlew clean test
```

Windows: `gradlew.bat`. Optional:

```bash
RUN_ALERTMANAGER_TEST=true ./gradlew test --tests AlertmanagerDiscordIntegrationTest
```

## Layout

```text
backend/
  src/main/java/dev/jotaeme/branchhub/
  src/main/resources/db/migration/   # Flyway SQL (see "Database migrations")
  build.gradle
  Dockerfile    # Spring Boot only (compose: build.context ./backend)
```

## Related docs

- [Project README (root)](../README.md)
- [Getting started](../docs/getting-started.md)
- [Architecture](../docs/architecture.md)
- [Documentation index — roadmap & Postman](../docs/README.md)
- [Frontend](../frontend/README.md)
