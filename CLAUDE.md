# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository shape

Branch Hub: a monorepo with a Spring Boot API (`backend/`, Java 25, Spring Boot 4, Gradle), an Angular 21 SPA (`frontend/`) and an observability stack (`monitoring/`: Prometheus, Grafana, Alertmanager → Discord). The HTTP contract under `/api/v1` is the boundary between them. Run Docker Compose and git from the repo root; run Gradle from `backend/` and npm from `frontend/`.

Detailed docs are in `backend/README.md`, `frontend/README.md` and `docs/` (`docs/getting-started.md` covers running the project, `docs/architecture.md` the design, `docs/roadmap/status.md` tracks what is done and what is next, `docs/roadmap/evolution.md` lists the planned v1.x waves, and `docs/security.md` covers security). Write all code, comments, commits and docs in English.

## Commands

```bash
# Full stack (needs .env; JWT_SECRET and APP_SUPER_ADMIN_PASSWORD are required)
cp .env.example .env
docker compose up -d --build          # all on 127.0.0.1: SPA :18090 (nginx), API :18091, Grafana :18092, Prometheus :18093, Alertmanager :18094, Postgres :18095
docker compose logs -f web app        # Compose project `branch-hub`, containers `branch-hub-*`, database `branchhub`

# Backend (from backend/)
./gradlew clean test
./gradlew test --tests BranchFlowIntegrationTest                 # single class
./gradlew test --tests 'dev.jotaeme.branchhub.application.branch.*'  # package
./gradlew bootRun                                                # needs DB_URL=jdbc:postgresql://localhost:18095/branchhub when using the Compose Postgres (see docs/getting-started.md)
RUN_ALERTMANAGER_TEST=true ./gradlew test --tests AlertmanagerDiscordIntegrationTest  # opt-in

# Frontend (from frontend/)
npm ci
npm start                     # http://localhost:4200, proxies /api to http://localhost:18091 (the Compose API)
npm run build                 # production build + scripts/patch-index-html.mjs (do not skip the patch step)
npm test                      # Vitest via ng test
npx ng test --include src/app/path/to/file.spec.ts   # single spec
npm run test:e2e:smoke        # Playwright, against E2E_BASE_URL (default http://localhost:18090, i.e. Compose)
npm run test:e2e:admin-smoke
npm run openapi:generate      # regenerate typings from http://localhost:18091/v3/api-docs (backend must be running)
```

Host ports live in the block 18090–18099, all bound to `127.0.0.1`; Kafka and blackbox have none. The server runs a single stack, the published demo, on the prod overlay (its `.env` sets `COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml`), so day-to-day dev runs `npm start` / `bootRun` against the stack's host ports instead of a second stack.

Every test that starts a Spring context extends `support/AbstractPostgresIntegrationTest`, which shares one Testcontainers PostgreSQL (wired with `@ServiceConnection`) across the whole run; Flyway builds its schema and there is no H2. Docker must be running for `./gradlew test`. The test `application.properties` replaces the main one, so it sets `ddl-auto=validate` itself. The test config (`src/test/resources/application.properties`) seeds a SUPER_ADMIN with `admin` / `admin123` and disables notifications.

## Backend architecture

The code uses Clean Architecture under `dev.jotaeme.branchhub` (main class `BranchHubApplication`):

- `domain`: entities and core rules.
- `application`: use cases (`*/usecase`), ports (`*/port`), models, and framework-agnostic pagination (`common/pagination`) and exceptions. It must not depend on Spring Data or web types.
- `infrastructure`: adapters that implement the ports (JPA under `persistence/`, IAM persistence under `persistence/iam`), security (JWT, filters), Kafka notification, logging, metrics and config.
- `entrypoints/rest`: controllers, DTOs, mappers and the HTTP pagination binding.

Key flows that span several layers:

- **Branch writes use a transactional outbox.** A use case persists `branch` and `outbox_event` in one transaction. A relay publishes to Kafka, and the consumer is idempotent through `processed_event`, with retries and a DLT. New domain events should follow the same pattern.
- **Pagination:** `PageRequestParams` (HTTP, validated) → `PaginationBinding` → `PageQuery` plus a per-resource `SortPolicy` (e.g. `BranchListPagination.SORT_POLICY`). The repository returns a `PageSlice`, the use case a `PageResult`, and `PageMapper` turns it into `PagedResponse`. `SpringPageRequests` is the only place that builds a Spring `PageRequest`. Defaults: size 10, max 100, `sort=prop,asc|desc`.
- **Errors** are RFC 7807 Problem Details (`type`, `title`, `status`, `detail`, `instance`, `timestamp`, optional `errors`).
- **Auth:** login returns a short-lived access JWT in the response body and sets an HttpOnly refresh cookie. `POST /api/v1/auth/refresh|logout` act on that cookie. Access rules: `/api/v1/admin/**` is SUPER_ADMIN only (enforced in the security config and by `@PreAuthorize` on admin controllers), `/api/v1/branches/**` allows SUPER_ADMIN or APP_ADMIN, and `GET /api/v1/me` needs any authenticated user. On startup the app seeds a SUPER_ADMIN (`APP_SUPER_ADMIN_EMAIL`/`_PASSWORD`) if no account with that email exists.
- **Profiles:** `docker`, `prod` (no Swagger, reduced actuator) and `debug` in `src/main/resources/application-*.properties`. The prod overlay `docker-compose.prod.yml` turns Swagger back on, exposes `prometheus` on the private network, sets the `Secure` cookie, makes Grafana anonymous Viewer and puts `web` and `grafana` on the external `edge` network (published through `cloudflared` as `code-branchhub.jotaeme.dev` and `code-branchhub-grafana.jotaeme.dev`). Off the server: `docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d --build`.
- **Flyway** migrations live in `src/main/resources/db/migration/V{n}__desc.sql`. Flyway owns the schema in every profile (`spring-boot-starter-flyway`); Hibernate only runs `ddl-auto=validate`. `V1__init_schema.sql` is the frozen baseline: never edit an applied `V*` script, add a new version instead.
- Custom Micrometer metrics use the `app_*` prefix (see `backend/README.md`). Prometheus alert rules in `monitoring/` (group `branch-hub-alerts`, job `branch-hub-api`) depend on them, so renaming a metric breaks alerts. The Grafana dashboard is `monitoring/grafana/dashboards/branch-hub-observability.json`.

## Frontend architecture

The SPA uses standalone components, signals for local state and lazy `loadChildren` routes (`admin`, `branches`). Folders: `core/` (services, auth, interceptors, i18n, API models), `features/` (pages), `layout/` (shell) and `shared/` (UI components, pipes, utils). Styling is Tailwind v4 plus DaisyUI v5 (`light`/`dark` themes via `data-theme`).

- **Typed API contract:** `core/models/openapi.generated.ts` is generated, so never edit it by hand. `core/models/api-types.ts` holds the app-facing aliases. `core/api/api-paths.ts` (`ApiPaths` via `apiPath<keyof paths>()`) makes a renamed backend path fail the TS build. Services call `apiUrl(API_BASE_URL, ApiPaths.…)`. After a backend contract change, run `openapi:generate` and update the Postman collection in `docs/postman/branch-hub.postman_collection.json`.
- **HTTP interceptors** (`app.config.ts`) run in a fixed order: `apiInterceptor` → `authErrorInterceptor` → `refreshInterceptor`. On a 401, `refreshInterceptor` refreshes through the cookie (using `HttpBackend`) and retries the request once. The access token is kept only in memory.
- **Server state** goes through TanStack Query (`core/query/query-client.factory.ts`: 5 min `staleTime`, refetch on window focus). Mutations invalidate the related query keys, and login/logout clear user-scoped queries such as `me`.
- **i18n** uses an in-house `I18nService` with static `en`/`es` dictionaries, not ngx-translate or `$localize`. Add new strings to both locales. Route `data['titleKey']` sets the tab title. Persisted preferences use the `branch-hub.pref.v1` prefix (`core/preferences/preference-keys.ts`, kept in sync with `public/theme-init.js`).
- **CSP:** `frontend/nginx.conf` sets a strict `script-src 'self'`, so inline scripts are not allowed (the theme bootstrap lives in `public/theme-init.js`). If the browser needs to reach new origins, update `connect-src`. Nginx also proxies `/api`, `/swagger-ui`, `/v3/*` and only `/actuator/health` to the `app` container so the browser stays same-origin; every other `/actuator/*` path returns 404, and `X-Forwarded-Proto` keeps the scheme `cloudflared` sends.

## Workflow conventions

- Per `docs/roadmap/overview.md`, the user runs git commands themselves. Propose the exact branch/add/commit/push commands (Conventional Commits, e.g. `feat(iam): …` on a `feat/...` branch) instead of running them, unless asked to.
- When closing a phase or feature, update the module README(s), `docs/roadmap/status.md` and `CHANGELOG.md`, and give verification commands.
