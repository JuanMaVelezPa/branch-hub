# Changelog

All notable changes to Branch Hub are documented here. Version labels **`v1.x`** are the release numbers of this project and follow the waves in [docs/roadmap/evolution.md](docs/roadmap/evolution.md). They are not npm or Gradle artifact versions.

## Unreleased

**Public demo access (SPEC 05).** Visitors can try the published app with a shared `APP_ADMIN` account, on sample data that resets every week.

- **API:**
  - New `GET /api/v1/public/demo` (no auth): `200 {email, password, grafanaUrl}` when `DEMO_ENABLED=true`, otherwise `404` Problem Details.
  - With the demo on, a runner seeds the demo user (email verified, enabled, role `APP_ADMIN` only) if missing, and failed logins for the demo email no longer count toward the lockout. Every other account keeps it.
  - `DEMO_*` env vars bind to `app.demo.*`, all off by default. A blank `DEMO_USER_EMAIL` or `DEMO_USER_PASSWORD` with the demo on stops startup with a message naming the variable.
- **Reset job** (`DEMO_RESET_ENABLED=true`): every Saturday at 10:00 `America/Bogota` by default (`DEMO_RESET_CRON`, `DEMO_RESET_ZONE`). In one transaction it deletes every `branch`, `outbox_event` and `processed_event` row, inserts 12 sample branches (`BR-001`–`BR-012`, 2 inactive, from `demo/demo-branches.csv`), and restores the demo user (enabled, unlocked, env password, roles exactly `{APP_ADMIN}`). It never touches other users, roles or `iam_audit_log`, and publishes nothing to Kafka. It also runs once at startup when `branch` is empty. New metric `app_demo_resets_total{outcome}`.
- **SPA:** a "Try the demo" card on the login page, shown only on a 200: the credentials, a "Fill in" button (fills the form, does not submit), and links to Swagger and Grafana. `en` and `es` strings. Typings regenerated, with a new `DemoAccess` alias and `ApiPaths.publicDemo`.
- **Fixes:**
  - The login form stayed disabled after submitting an email that failed the app's email check.
  - `npm start` now also proxies `/swagger-ui.html`, `/swagger-ui/**` and `/v3/**`.
  - Scheduling no longer depends on `app.notifications.enabled` for the demo reset.
- **Ops:** `docker-compose.prod.yml` passes the `DEMO_*` vars and `.env.prod.example` documents them. **Action needed** on the server: add the `DEMO_*` values to `.env` (the real demo password lives only there). The first reset deletes every existing branch; back up first if any matter.
- **Tests:** Testcontainers integration tests for the demo user, the lockout exemption, the endpoint, the reset (including rollback) and the scheduler and metric; Vitest specs for the card, the service and the login fix. The test Hikari pool is capped at 4 connections.
- **Docs:** how to try the demo in the [README](README.md#try-the-demo), why the credentials are public in [security](docs/security.md#the-public-demo-account-spec-05), properties and job in the [backend README](backend/README.md#public-demo), the card in the [frontend README](frontend/README.md#public-demo-card-login), and a "Public demo" request in the Postman collection.

**Published Branch Hub through the shared ingress (SPEC 04).** Configuration only: no application code or API contract change.

- **Host ports:** every port in `docker-compose.yml` is bound to `127.0.0.1` in the block 18090–18095: web `WEB_PORT=18090`, API `API_PORT=18091`, Grafana `GRAFANA_PORT=18092`, Prometheus `PROMETHEUS_PORT=18093`, Alertmanager `ALERTMANAGER_PORT=18094`, PostgreSQL `POSTGRES_PORT=18095`. Kafka (9092) and the Blackbox exporter (9115) no longer publish a host port. **Action needed:** remove `WEB_PORT=8080` / `API_PORT=8081` from your `.env`, recreate the stack and update SSH forwards and database clients (see [getting started](docs/getting-started.md#ports-and-urls)).
- **Prod overlay** (`docker-compose.prod.yml`), the stack the server runs through `COMPOSE_FILE` in its `.env`:
  - `web` and `grafana` join the external `edge` network that `cloudflared` uses. Public URLs: `https://code-branchhub.jotaeme.dev` (SPA, `/api`, Swagger) and `https://code-branchhub-grafana.jotaeme.dev`.
  - `app`: `Secure` refresh cookie, Swagger and API docs on, actuator `health,info,prometheus` open on the private network so Prometheus can scrape it.
  - Grafana: anonymous `Viewer`, login form and sign-up disabled, `GF_SERVER_ROOT_URL` from `GRAFANA_PUBLIC_URL`.
- **Nginx** (`frontend/nginx.conf`):
  - `X-Forwarded-Proto` keeps the incoming header (`https` from `cloudflared`) and falls back to `$scheme`.
  - `location ^~ /swagger-ui/` fixes Swagger through `web`: its assets used to fall into the static-asset regex and return 404.
  - Only `/actuator/health` is proxied; every other `/actuator/*` path returns 404.
- **Alertmanager:** the committed `alertmanager.yml` reads the Discord webhook from `webhook_url_file`, like the local copy, so the prod overlay sends real alerts.
- **Env templates:** `.env.example` and `.env.prod.example` list the block ports (commented, the compose file holds the defaults). `.env.prod.example` adds `COMPOSE_FILE`, `SECURITY_COOKIES_SECURE=true` and `GRAFANA_PUBLIC_URL`. `.gitignore` now also ignores `.env*.bak` (the server keeps its old dev `.env` as `.env.dev.bak`).
- **Dev tooling:** `npm start` proxies to `http://localhost:18091`, Playwright defaults to `http://localhost:18090`, `openapi:generate` reads `http://localhost:18091/v3/api-docs`, and the Postman `baseUrl` is `http://localhost:18091`.
- **Docs:** ports, the server mode and the `edge` prerequisite in [getting started](docs/getting-started.md#server-mode-the-prod-overlay); what is public and why in [security](docs/security.md#8-what-is-public-on-the-published-demo-and-why).

**Gave the SPA its own visual identity (SPEC 03).** Frontend only: no backend or API contract change.

- **Identity:**
  - DaisyUI's `light` and `dark` themes are overridden with the "wayfinding" tokens (route green `#0E7A5E` / `#3FBF94`, signal amber, paper and ink), in OKLCH, with every color token set explicitly. Theme names and stored preferences are unchanged.
  - Overpass Variable is self-hosted through `@fontsource-variable/overpass` and is the base font. Counts, codes and dates use tabular numerals.
  - New branching-node logo (`shared/ui/logo`), also used as `favicon.svg`.
- **Shell:** a sidebar (Dashboard, Branches, Admin, Profile) with a topbar replaces the top navbar. On `lg` and up the sidebar collapses to an icon rail, saved under `branch-hub.pref.v1.sidebar`. Below `lg` it is a drawer opened from a hamburger.
- **Login:** split screen with a route-map drawing that draws itself in once (still under reduced motion), the logo and the tagline "Every branch's details, in one place."
- **Dashboard:** new `/dashboard` route is the post-login home, and `/`, the login, `guestGuard` and `roleGuard` redirect there. It shows a summary sentence with live counts (singular and plural forms in both locales), "Add branch" and "Create user" quick actions, and, for SUPER_ADMIN, the latest 5 audit events as a timeline. An APP_ADMIN dashboard sends no `/api/v1/admin/**` request.
- **Screens:** every screen, the create-user modal and the shared UI components follow the new design rules. Below `sm`, the branches list, admin users and audit log show one card per row, with a sort select. Inactive branches show an amber marker plus the word "Inactive".
- **Copy:**
  - Sentence case everywhere. "New branch" is now "Add branch" (`data-testid="branch-add"`), and `appTitle` is "Branch Hub".
  - Successful mutations now show a toast that keeps the action's verb (Branch added, Changes saved, Branch deactivated, User created, Roles saved, User enabled/disabled).
  - Empty states name the next action, and error titles say what failed and what to do.
  - The dictionaries moved to `core/i18n/`. `es` is typed against `en`, and a unit spec checks that both have the same keys and placeholders.
- **Bundle:** the Spanish dictionary is lazy-loaded (the app initializer waits for it when Spanish is stored), and the DaisyUI `include` list drops seven unused components. The initial bundle is 568.46 kB, against 561.28 kB on `main`; the `initial` warning budget goes from 560 kB to 600 kB.
- **TypeScript 6 readiness:** `tsconfig.app.json` and `tsconfig.spec.json` set `rootDir: ./src` explicitly, which TypeScript 6 (and editors that bundle it) now requires.
- **Self-critique pass:** removed the always-visible sort icon from idle column headers. The hint now shows only on hover or keyboard focus.
- **Tests:** unit specs for the sidebar preference, the audit action labels, relative time and the dictionaries. The smoke suites use the new `nav-*` and `branch-add` test IDs. New `npm run test:e2e:screenshots` captures every screen at 390 and 1280 px in light and dark into the gitignored `frontend/e2e/__screenshots__/`.
- **Docs:** [frontend/README.md](frontend/README.md#design-direction) documents the design direction, tokens with contrast ratios, rules, shell behavior and the screenshot command. The root README screenshots in `docs/images/` are refreshed, with a new `dashboard.png`. `DOCS_IMAGES=1 npm run test:e2e:screenshots` regenerates them.

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
