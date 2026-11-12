# SPEC 05 — Public demo access

> **Status:** Draft
> **Depends on:** SPEC 04
> **Date:** 2026-11-11
> **Objective:** Let any visitor of `code-branchhub.jotaeme.dev` try the app with a public demo APP_ADMIN account, whose credentials and tool links the login page shows from a new public endpoint, on sample data that a scheduled job resets every night.

## Why this spec exists

SPEC 04 publishes the SPA, Swagger and Grafana, but only the owner can log in. Visitors see a login screen with no way in. This project exists to show the code, so visitors should be able to use the app as an APP_ADMIN (branch CRUD), while `/admin` stays SUPER_ADMIN only.

Public write access means visitors can rename, add or deactivate branches. The demo data must come back to a known state on its own, without the owner doing anything.

What exists today:

- `IamBootstrapRunner` seeds roles and one SUPER_ADMIN (which also has APP_ADMIN) from `APP_SUPER_ADMIN_EMAIL`/`_PASSWORD` when no account with that email exists.
- APP_ADMIN can list, read, create, update and deactivate branches. Branches have no delete. Users have no self-service password change, so a visitor cannot lock the demo account.
- Scheduling is already enabled (`@EnableScheduling` in `KafkaOutboxConfig`, used by the outbox relay).
- Use cases are plain classes wired in `ApplicationUseCaseConfig`. `application` must not depend on Spring Data or web types.

## Scope

**In:**

- **Demo configuration** (`app.demo.*`), all off by default. The prod overlay turns them on from `.env`.
- **Demo user seeding.** When `app.demo.enabled=true`, a runner creates the demo user if missing: email and password from env, email verified, enabled, role **APP_ADMIN only**.
- **Public endpoint** `GET /api/v1/public/demo`, `permitAll`:
  - Enabled: `200` with `{ email, password, grafanaUrl }`.
  - Disabled: `404` Problem Details.
- **Login demo card** in the SPA, shown only when the endpoint returns 200:
  - The demo email and password, with a "Fill in" button that fills the login form (it does not submit).
  - A link to `/swagger-ui.html`, and a link to Grafana when `grafanaUrl` is not blank.
  - Nothing is shown, and no error is shown, when the endpoint returns 404 or fails.
- **Sample branches:** 12 branches (codes `BR-001` to `BR-012`, several cities, 2 inactive), in a committed SQL resource.
- **Nightly reset job**, enabled only by `app.demo.reset.enabled=true`, cron `app.demo.reset.cron` (default `0 0 4 * * *`) in zone `app.demo.reset.zone` (default `UTC`). In one transaction it:
  1. Deletes every row of `processed_event`, `outbox_event` and `branch`.
  2. Inserts the sample branches.
  3. Restores the demo user: enabled, password from env, roles exactly `{APP_ADMIN}`. If the user is missing, it is recreated.
- **Seed on startup:** when the reset is enabled and `branch` is empty at `ApplicationReadyEvent`, the same reset runs once, so a fresh volume starts with sample data. A non-empty table is left alone, so a deploy does not wipe visitors' changes before the night.
- **Metric** `app_demo_resets_total{outcome="success|failure"}` and one INFO log line per reset (or ERROR with the cause).
- **Contract follow-up:** `npm run openapi:generate`, a `DemoAccess` alias in `api-types.ts`, an `ApiPaths` entry, and a "Public demo" request in the Postman collection.
- Prod overlay and `.env.prod.example`: `DEMO_ENABLED`, `DEMO_USER_EMAIL`, `DEMO_USER_PASSWORD`, `DEMO_GRAFANA_URL`, `DEMO_RESET_ENABLED`, `DEMO_RESET_CRON`, `DEMO_RESET_ZONE`.
- i18n strings for the card in `en` and `es`.
- Docs: `README.md` (how to try the demo, public URLs), `docs/security.md` (why the credentials are public, what the demo role can and cannot do, the reset), `backend/README.md` (properties, job, metric), `frontend/README.md` (demo card), `docs/roadmap/status.md`, `CHANGELOG.md`.

**Out of scope (for future specs):**

- A read-only role or permission.
- Rate limiting, CAPTCHA, abuse detection on login or writes.
- Resetting IAM data other than the demo user (other users, roles, `iam_audit_log`), or the SUPER_ADMIN.
- An on-demand "reset now" button or admin endpoint.
- Per-visitor sandboxes or isolated data.
- A Grafana panel or alert rule for `app_demo_resets_total` (the metric is exported; dashboards and alerts come later).
- Showing the demo card anywhere other than the login page.

## Data model

No schema change and no new Flyway migration. Reset and seed act on the existing `branch`, `outbox_event`, `processed_event`, `iam_user` and `iam_user_role` tables.

### Configuration (`application.properties`)

```properties
app.demo.enabled=${DEMO_ENABLED:false}
app.demo.user.email=${DEMO_USER_EMAIL:}
app.demo.user.password=${DEMO_USER_PASSWORD:}
app.demo.grafana-url=${DEMO_GRAFANA_URL:}
app.demo.reset.enabled=${DEMO_RESET_ENABLED:false}
app.demo.reset.cron=${DEMO_RESET_CRON:0 0 4 * * *}
app.demo.reset.zone=${DEMO_RESET_ZONE:UTC}
```

With `app.demo.enabled=true`, a blank email or password fails startup with a clear message. `app.demo.reset.enabled=true` requires `app.demo.enabled=true`.

### API

`GET /api/v1/public/demo` (no auth)

```json
{
  "email": "demo@branchhub.dev",
  "password": "<DEMO_USER_PASSWORD>",
  "grafanaUrl": "https://code-branchhub-grafana.jotaeme.dev"
}
```

`grafanaUrl` is an empty string when it is not set. Disabled → `404` RFC 7807 with `type`, `title`, `status`, `detail`, `instance` and `timestamp`.

### New and changed files

| Path | Change |
| --- | --- |
| `backend/.../application/demo/model/DemoAccess.java` | Record `email`, `password`, `grafanaUrl`. |
| `backend/.../application/demo/model/DemoBranch.java` | Record `code`, `name`, `city`, `active`. |
| `backend/.../application/demo/port/DemoDataPort.java` | `resetBranches(List<DemoBranch>)`, `restoreDemoUser(email, rawPassword)`, `isBranchTableEmpty()`. |
| `backend/.../application/demo/port/DemoSampleDataPort.java` | `List<DemoBranch> sampleBranches()`. |
| `backend/.../application/demo/usecase/GetDemoAccessUseCase.java` | Returns `Optional<DemoAccess>` from config. |
| `backend/.../application/demo/usecase/ResetDemoDataUseCase.java` | Runs the three reset steps through the ports, in one transaction (the transaction boundary is set where the bean is wired, not in `application`). |
| `backend/.../infrastructure/demo/DemoProperties.java` | `@ConfigurationProperties("app.demo")` with startup validation. |
| `backend/.../infrastructure/demo/DemoDataResetScheduler.java` | `@Scheduled(cron, zone)` and `ApplicationReadyEvent` seed. `@ConditionalOnProperty(app.demo.reset.enabled)`. Increments the metric and logs. |
| `backend/.../infrastructure/persistence/demo/JdbcDemoDataAdapter.java` | Implements `DemoDataPort` (deletes, inserts, demo-user restore with `PasswordEncoder`). |
| `backend/.../infrastructure/persistence/demo/ClasspathDemoSampleDataAdapter.java` | Reads `demo/demo-branches.csv`. |
| `backend/src/main/resources/demo/demo-branches.csv` | The 12 sample branches. |
| `backend/.../infrastructure/security/DemoUserBootstrapRunner.java` | Creates the demo user at startup when enabled and missing. Runs after `IamBootstrapRunner` (roles exist). |
| `backend/.../infrastructure/config/ApplicationUseCaseConfig.java` | Wires the two use cases. |
| `backend/.../infrastructure/config/SecurityConfig.java` | `permitAll` on `GET /api/v1/public/demo`. |
| `backend/.../entrypoints/rest/PublicDemoController.java` + `dto/DemoAccessResponse.java` | The endpoint. |
| `backend/src/main/resources/application.properties` | The `app.demo.*` properties. |
| `docker-compose.prod.yml`, `.env.prod.example` | `DEMO_*` env vars. |
| `frontend/src/app/core/demo/demo-api.service.ts` | `getDemoAccess()` mapping 404 → `null`. |
| `frontend/src/app/core/demo/demo-query.keys.ts` | `['demo-access']`, no retry, long `staleTime`. |
| `frontend/src/app/features/auth/login.component.{ts,html}` | The demo card and "Fill in". |
| `frontend/src/app/core/models/openapi.generated.ts` | Regenerated, never hand-edited. |
| `frontend/src/app/core/models/api-types.ts`, `core/api/api-paths.ts` | `DemoAccess` alias, `ApiPaths.publicDemo`. |
| `frontend/src/app/core/i18n/translations.ts`, `translations.es.ts` | Card strings. |
| `docs/postman/branch-hub.postman_collection.json` | "Public demo" request. |

The sample data format (CSV vs SQL) and these class names may be adjusted during implementation if a cleaner fit appears in the existing code. Any change is recorded in this spec's decisions.

## Implementation plan

Each step leaves the app building and the tests green.

1. **Config and demo user.** `DemoProperties`, the properties, `DemoUserBootstrapRunner`. Test (Testcontainers): with demo on, the user exists, is enabled, and has exactly `{APP_ADMIN}`. A second start does not duplicate it. With demo off, nothing is created.
2. **Public endpoint.** `GetDemoAccessUseCase`, `PublicDemoController`, DTO, `permitAll`. Tests: enabled → 200 with the three fields and no auth. Disabled → 404 Problem Details.
3. **Reset use case and adapters.** Ports, `ResetDemoDataUseCase`, the JDBC and classpath adapters, the CSV. Test: after creating an extra branch, renaming one, deactivating one, adding an outbox row, disabling the demo user and adding SUPER_ADMIN to it, the reset leaves exactly the 12 sample branches, empty `outbox_event` and `processed_event`, and the demo user enabled with `{APP_ADMIN}` and the env password (login works). The SUPER_ADMIN, other users and `iam_audit_log` are unchanged.
4. **Scheduler, startup seed and metric.** `DemoDataResetScheduler`. Tests: with an empty `branch` table and reset on, startup seeds 12 branches. With a non-empty table, startup leaves it. `app_demo_resets_total{outcome="success"}` increments after a reset. The cron itself is not waited on: the test calls the scheduled method.
5. **Contract and SPA.** Run the backend, `npm run openapi:generate` (with `OPENAPI_SPEC_URL=http://localhost:18091/v3/api-docs`), add the alias, `ApiPaths`, service, query key, login card and i18n strings. Specs: card hidden on 404 and on network error, shown with values on 200, "Fill in" sets both form fields, Grafana link hidden when `grafanaUrl` is blank. Check: `npm test` and `npm run build` pass.
6. **Prod overlay and docs.** `DEMO_*` in `docker-compose.prod.yml` and `.env.prod.example`, Postman request, docs listed in Scope.
7. *(owner)* **Deploy.** Add the `DEMO_*` values to the server `.env`, then `docker compose up -d --build`. Check the acceptance criteria on `https://code-branchhub.jotaeme.dev`.
8. **Close.** `docs/roadmap/status.md`, `CHANGELOG.md`, proposed git commands.

## Acceptance criteria

- [ ] With `DEMO_ENABLED` unset, `GET /api/v1/public/demo` returns 404 Problem Details and no demo user is created.
- [ ] On the server, `GET https://code-branchhub.jotaeme.dev/api/v1/public/demo` returns 200 with `email`, `password` and `grafanaUrl` without a token.
- [ ] The login page on the public site shows the demo card. "Fill in" fills both fields, and logging in lands on the dashboard as an APP_ADMIN.
- [ ] Logged in as the demo user, `/admin` is not in the sidebar, and `GET /api/v1/admin/users` returns 403.
- [ ] The card links to `/swagger-ui.html` and to `https://code-branchhub-grafana.jotaeme.dev`, and both open.
- [ ] Locally with the demo off, the login page shows no card and no error.
- [ ] After a fresh volume, the branch list shows exactly the 12 sample branches (`BR-001` to `BR-012`, 2 inactive).
- [ ] After changing data as the demo user and triggering the reset (integration test, or a temporary `DEMO_RESET_CRON` a few minutes ahead on the server), the list is back to the 12 sample branches and the demo login still works.
- [ ] The reset does not touch the SUPER_ADMIN, other users, roles or `iam_audit_log`.
- [ ] `app_demo_resets_total` is visible at `http://127.0.0.1:18093` after a reset.
- [ ] `./gradlew clean test`, `npm test` and `npm run build` pass. `openapi.generated.ts` was regenerated, not edited by hand.
- [ ] The Postman collection has the "Public demo" request, and `README.md` explains how to try the demo.
- [ ] No real demo password in any committed file. It lives only in the server `.env`.

## Decisions taken and discarded

- **Demo APP_ADMIN with public credentials**, over a read-only role (a domain change that needs its own spec) or no public login (visitors could not try the app). APP_ADMIN has no delete and no self-service password change, so the worst a visitor can do is edit branches, which the reset undoes.
- **Credentials from a public endpoint**, over hard-coding them in the SPA (two copies that drift, and a card in every environment) or listing them only in the README (visitors who land on the site would not see them). The card shows only where the demo is on. The cost is an API contract change.
- **Grafana URL from the same endpoint**, so local dev, where Grafana is not public, shows no dead link.
- **Spring `@Scheduled` job**, over a host crontab with a SQL script (lives outside the app and needs host setup) or a scheduler container (one more container). It is versioned, testable with Testcontainers, and uses the scheduling that is already enabled.
- **Reset by deleting rows, not through the use cases or the outbox.** The reset is maintenance, not a domain event, so it publishes nothing to Kafka. Clearing `outbox_event` and `processed_event` keeps the outbox consistent with the reset table.
- **The reset also restores the demo user** (enabled, password, `{APP_ADMIN}`). A SUPER_ADMIN mistake, such as disabling it or adding roles, fixes itself overnight.
- **Seed on startup only when `branch` is empty**, so a fresh volume has data, but a redeploy does not wipe what visitors did that day.
- **Default cron 04:00 UTC**, overridable by `DEMO_RESET_CRON` and `DEMO_RESET_ZONE`.
- **Split from SPEC 04.** SPEC 04 is configuration only. This spec holds the Java and Angular feature work.

## Identified risks

- **Visitor vandalism between resets.** Offensive branch names stay visible until 04:00. Accepted for a portfolio demo. Rate limiting or moderation would be a later spec.
- **Spam writes.** A script could create thousands of branches in a day. There is no rate limit (out of scope). The nightly delete still clears them. Cloudflare's default protections apply meanwhile.
- **A reset during an in-flight Kafka event.** An event already published but not yet consumed can arrive after `processed_event` was cleared, so the consumer handles it once for a branch that no longer exists. The consumer only logs or notifies, so this is harmless. The test does not cover it.
- **Startup validation blocks the app.** A typo in `DEMO_USER_*` with `DEMO_ENABLED=true` stops the API, and the public site goes down. This is deliberate (fail fast), and the error message names the missing variable. Step 7 checks the API logs right after deploy.
- **Public password in the API response.** That is the design. The demo user must never get any role other than APP_ADMIN, and the reset enforces it.
