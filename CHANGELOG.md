# Changelog

All notable changes to Branch Hub are documented here. Version labels **`v1.x`** are the release numbers of this project and follow the waves in [docs/roadmap/evolution.md](docs/roadmap/evolution.md). They are not npm or Gradle artifact versions.

## Unreleased

**Flyway now owns the database schema (SPEC 02).**

- **Schema:**
  - Added `spring-boot-starter-flyway`, which Spring Boot 4 needs to run Flyway. Migrations now run on every startup, in every profile.
  - Replaced `V1`–`V4` with one baseline, `V1__init_schema.sql`. Flyway had never run anywhere, so no checksum breaks. The baseline is frozen from now on.
  - Dropped the redundant `iam_user_email_idx`; the unique constraint on `iam_user.email` already indexes it.
  - The `docker` profile no longer sets `ddl-auto=update`, so every profile uses `validate`.
  - **Action needed:** a local database Hibernate created has no Flyway history. Run `docker compose down -v` once (see [getting started](docs/getting-started.md#database-urls)).
- **Tests:**
  - Testcontainers moved from the pinned `1.21.4` to the `2.0.4` that Spring Boot manages, with `@ServiceConnection`.
  - Every Spring context test runs on one shared PostgreSQL container built by the migrations. H2 is removed, and `./gradlew test` needs Docker.
  - The integration tests no longer create their own tables, and `BranchHubApplicationTests` no longer excludes Boot 3 auto-configuration classes that do not exist in Boot 4.
  - New `FlywayMigrationIntegrationTest` checks that version `1` is applied and every app table exists.

**Renamed the project from `spring-web` to `branch-hub` and prepared the repository for publishing.**

- **Rename:**
  - The Java base package moved from `com.jm.spring_web` to `dev.jotaeme.branchhub`. The main class is now `BranchHubApplication`, the Gradle project is `branch-hub` (group `dev.jotaeme`), and `spring.application.name` is `branch-hub`.
  - The database and user changed from `companydb` / `company` to `branchhub`. Existing local stacks need `docker compose down -v` (see [getting started](docs/getting-started.md#database-urls)).
  - Docker Compose now has the project name `branch-hub`, and all containers are `branch-hub-*`.
  - The Prometheus job is `branch-hub-api`, the alert group is `branch-hub-alerts`, and the alerts are `BranchHubApiDown` and `BranchHubApiFlapping`.
  - The Grafana dashboard is `branch-hub-observability`.
  - The SPA package is `branch-hub-web`, its title is "Branch Hub", and browser preferences use the `branch-hub.pref.v1` prefix. Old `spring-web.pref.v1` keys are not migrated, so theme and language reset once.
  - The Postman collection is now `docs/postman/branch-hub.postman_collection.json`.
  - The Problem Details `type` base URI is now `https://api.branch-hub/errors/`.
- **Fixes:**
  - `backend/gradlew` is executable.
  - The Grafana healthcheck now matches the pretty-printed `/api/health` response, so the container no longer stays `unhealthy`.
  - The SPA footer links to jotaeme.dev, GitHub and the current LinkedIn profile, and no longer shows an email address.
  - The Kafka broker in Compose sets the internal-topic replication factors to 1. Before, `__consumer_offsets` could not be created on the single broker, so the HR notification consumer never joined its group.
  - The Kafka and Blackbox containers have healthchecks, so every Compose service reports `healthy`.
  - `app_use_case_duration` publishes a percentile histogram, so the Grafana p95 panel has data.
  - The Grafana panel and the `FailedNotificationsIncreasing` alert use `failed_notifications_total`, the name Prometheus exposes. Before, the alert could never fire.
  - Failed logins are counted in Prometheus. `app_auth_login_total` now has the same tag keys for success (`reason="none"`) and failure, so the failure meter is no longer rejected.
  - The admin sub-navigation renders as tabs again: the DaisyUI `include` list in `styles.css` now has the `tab` component, and the markup uses the v5 class `tabs-box` instead of `tabs-boxed`.
- **Docs:**
  - Added an MIT [LICENSE](LICENSE), [docs/architecture.md](docs/architecture.md) with Mermaid diagrams, and [docs/getting-started.md](docs/getting-started.md).
  - Rewrote the root README and [docs/README.md](docs/README.md).
  - Rewrote the roadmap as project documentation.

## v1.1.0

**Closed:** IAM1 baseline (email/password identity, lockout, DB users) plus the current-user profile for the SPA.

- **Backend:** `GET /api/v1/me` returns id, email, enabled, lock state, `createdAt` and roles (any authenticated user).
- **Frontend:** route `/me`, shell nav **Profile** / **Perfil**, TanStack Query cache invalidation on login and logout.
- **Tooling:** Postman request **1.05 Me - Current profile** (after **1. Auth - Login**).
- **Docs:** `status.md` marks **v1.1** **Done**; roadmap index updated.

Tag:

```bash
git tag -a v1.1.0 -m "v1.1: IAM1 + GET /me"
```
