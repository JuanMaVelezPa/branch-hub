# Backend — Spring Web API

Spring Boot **4** service (Java **25**, Gradle). Clean Architecture: `domain` → `application` → `infrastructure` → `entrypoints`.

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

- Tests that need a real schema use the same Flyway-managed schema (e.g. Testcontainers) so migrations stay the **single source of truth**.

## Run with Docker (full stack)

From **repo root**:

```bash
cp .env.example .env
cp monitoring/alertmanager/alertmanager.local.yml.example monitoring/alertmanager/alertmanager.local.yml
cp monitoring/alertmanager/discord-webhook-url.example monitoring/alertmanager/discord-webhook-url
docker compose up -d --build
```

Configure `JWT_SECRET`, `APP_SUPER_ADMIN_PASSWORD`, and (optional) Discord webhook per root `.env.example`.

**URLs (Docker Compose on the host):**

| Service | Host port | Notes |
|--------|-----------|--------|
| **`web`** (Nginx + Angular) | **8080** | Browser entry. Proxies `/api`, `/actuator`, `/swagger-ui`, `/v3/*` to `app` so the UI keeps a single origin. |
| **`app`** (Spring Boot) | **8081** | Direct API: Postman, `curl`, Swagger UI at http://localhost:8081/swagger-ui.html, Actuator, etc. |
| PostgreSQL | **5433** | Exposed for local tooling (psql, migrations debugging). Inside Compose network the hostname is `postgres:5432`. |
| Grafana | 3000 | |
| Prometheus | 9090 | Scrapes **`app:8080`** on the Compose network (not via the host port). |

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

- Public: `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`, `/api/v1/auth/mfa/verify`, Swagger/OpenAPI (non-prod), `GET /actuator/health`, `GET /actuator/info`
- Optional public: `/actuator/metrics/**`, `/actuator/prometheus` (configurable)
- **SUPER_ADMIN:** `/api/v1/admin/**` (users, roles, **audit log** `GET /api/v1/admin/audit-logs`)
- **SUPER_ADMIN / APP_ADMIN:** `/api/v1/branches/**`
- **Authenticated (any role):** `GET /api/v1/me` (current user profile: id, email, roles, …); **`/api/v1/auth/sessions`** (list refresh sessions, revoke one, revoke other devices — JWT + refresh cookie where noted)
- **SUPER_ADMIN / APP_ADMIN (bearer):** `POST /api/v1/auth/mfa/enroll/begin`, `POST /api/v1/auth/mfa/enroll/confirm` — register **TOTP** (requires `security.mfa.encryption-key-base64` / `IAM_MFA_ENCRYPTION_KEY` on the server)
- Other routes: authenticated

Spring does **not** serve the static UI; the **`web`** container (Nginx) does on host port **8080**. The API is also reachable **directly** on host port **8081** for tools and clarity.

**Auth:** login sets HttpOnly refresh cookie when the flow completes with tokens; access token in response body (SPA keeps it in memory). If the user is **SUPER_ADMIN** or **APP_ADMIN** and has **MFA (TOTP) enabled**, login returns only `mfaChallengeToken` (no refresh cookie) until `POST /api/v1/auth/mfa/verify` succeeds. `POST /api/v1/auth/logout` revokes the current refresh session server-side (when the cookie is sent) and clears the cookie (`204`). Login rate limiting applies **per client IP** and **per `username` in the JSON body** (normalized); see `security.auth-rate-limit.*` in `application.properties`.

**OpenAPI:** non-prod serves Swagger UI (`/swagger-ui.html`) and **`/v3/api-docs`**. Operations that need a JWT show a lock icon and use the **Authorize** dialog (`bearerAuth`); login, refresh, logout, and MFA verify stay public in the spec.

### IAM bootstrap (local)

On startup, the backend can seed a first **SUPER_ADMIN** account (if missing) so a fresh fork is usable without manual SQL.

- **Enable/disable:** `APP_IAM_BOOTSTRAP_ENABLED` (default `true`)
- **Seed email:** `APP_SUPER_ADMIN_EMAIL` (default `admin@example.com`)
- **Seed password:** `APP_SUPER_ADMIN_PASSWORD` (default `Admin_ChangeMe_2026!`)

For production, treat this as a **break-glass** path: rotate secrets and restrict access to env/config.

### IAM email outbox (transactional)

Inserts into **`iam_email_outbox`** (Flyway **V6**); a scheduled relay renders **Thymeleaf** templates from `src/main/resources/templates/email/` and sends via **`JavaMailSender`** (SMTP). Separate from the Kafka **`outbox_event`** used for domain notifications.

- **Enable:** `IAM_EMAIL_ENABLED=true` and set `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` (e.g. Gmail [app password](https://support.google.com/accounts/answer/185833)), plus `IAM_EMAIL_FROM`.
- **Enqueue from code:** inject `EmailOutboxPort` and call `enqueue(new EmailEnqueueCommand(EmailTemplateIds.GENERIC_NOTICE, to, subjectOrNull, variables))`.
- **Account re-enabled:** when an admin sets `enabled: true` on a previously disabled user, an **`ACCOUNT_ACTIVATED`** message is enqueued (template `templates/email/account-activated.html`).
- **New templates:** add `IamEmailTemplateId` + `templates/email/...html` (English by default).
- **Tests:** `app.iam.email.enabled=false` (see `src/test/resources/application.properties`).

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

`outbox_lag`, `failed_notifications`, `app_auth_login_total`, `app_branch_command_total`, `app_iam_admin_action_total`, `app_use_case_duration`, `app_outbox_publish_total`, `app_notification_consumer_total`.

## Alerting

Prometheus rules → Alertmanager → Discord (see `monitoring/`). Alerts include app down, Kafka/Postgres, outbox lag, consumer DLT, etc.

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
  src/main/java/com/jm/spring_web/
  src/main/resources/db/migration/   # Flyway SQL (see "Database migrations")
  build.gradle
  Dockerfile    # Spring Boot only (compose: build.context ./backend)
```

### MFA (TOTP) configuration

- **Key (required for enroll/verify):** `IAM_MFA_ENCRYPTION_KEY` — Base64 encoding of **32 bytes** (AES-256), or `security.mfa.encryption-key-base64` in properties.
- **Issuer label (authenticator apps):** `IAM_MFA_ISSUER` or `security.mfa.issuer` (defaults to `spring.application.name`).
- **Challenge JWT lifetime:** `JWT_MFA_CHALLENGE_SECONDS` or `security.jwt.mfa-challenge-expiration-seconds` (default 300s).

## Related docs

- [Repository hub (root)](../README.md)
- [Documentation index — roadmap & Postman](../docs/README.md)
- [Frontend](../frontend/README.md)
