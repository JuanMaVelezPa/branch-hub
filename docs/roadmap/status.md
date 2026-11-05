# Roadmap — Delivery status

Living checklist: update when a milestone closes. Technical detail stays in module READMEs.

**Ordered evolution (versions, waves, BE+FE together):** [**evolution.md**](evolution.md) — read this first for **what to do next** and **v1.x** release numbers.

## Delivery timeline (read this first)

### Done — **v1.0** baseline

| Step | Code | Scope | Status |
|------|------|--------|--------|
| **1** | **B1** | Backend — API maturity (OpenAPI, Problem Details, pagination, tests) | **Done** |
| **2** | **B2** | Backend — Security & operations (JWT, refresh cookie, Docker, metrics) | **Done** |
| **3** | **F1** | Frontend — First slice: shell, login, branch **list + create**, prefs, i18n, OpenAPI typings, unit + smoke E2E | **Done** |
| **4** | **F2** | Frontend — Second slice: branch **detail + update + deactivate** (API was already in B1) | **Done** |
| **5** | **H1** | Frontend **hardening**: silent **refresh** interceptor + **CSP** / security headers on Nginx (`web`) | **Done** |

### IAM & later waves — map to [evolution.md](evolution.md)

Work landed **out of strict wave order** in places (e.g. **F3** and **IAM2** shipped while IAM1 was still maturing). Use the tables below for **what is done vs what remains**; the version column is the **release label** used when a wave closes.

| Version | Wave | Codes (summary) | Closure status |
|---------|------|-----------------|----------------|
| **v1.1** | 1 | **IAM1** | **Done** — email/password identity, lockout, **`GET /api/v1/me`** + SPA **`/me`**; phone/OAuth → **IAM4**. |
| **v1.2** | 2 | **IAM2** (admin API + admin UI) | **Done** (super-admin IAM + audit read + method security). Optional multi-app scope remains YAGNI. |
| **v1.3** | 3 | **F3** | **Done** |
| **v1.4** | 4 | **IAM3** | Not started |
| **v1.5** | 5 | **IAM4** | Not started (forgot password, OTP, **phone**, Google OAuth, …) |
| **v1.6** | 6 | **IAM5** | Not started |
| **v1.7** | 7 | **IAM6** | Not started |

Detail per milestone: [auth-platform.md](auth-platform.md), [frontend.md](frontend.md). Cross-cutting: [security.md](../security.md).

---

## IAM implementation and gaps (this repo)

Single place for **code truth** vs **auth-platform.md** (design). Update this section when you close a gap.

### IAM1: Persistence and login

| Deliverable | Status |
|-------------|--------|
| Flyway **V3** `iam_user` / `iam_role` / `iam_user_role`; JWT **`sub`** = user id; bcrypt; bootstrap **SUPER_ADMIN** | **Done** |
| DB-backed `UserDetailsService` (`DbUserDetailsService`) | **Done** |
| Shared **password policy** (admin create + design for future flows) | **Done** |
| **Lockout** (`failed_login_count`, `locked_until`, configurable thresholds) | **Done** |
| §1.1 extras: **phone**, **OAuth** columns, **handle**, multi-anchor linking | **Deferred** → **IAM4+** (optional preparatory migration earlier if useful) |
| **Registration** (public sign-up) | **Not implemented** → optional **IAM2** polish or **IAM4** scope |
| **`GET /api/v1/me`** (profile for SPA) | **Done** — `MeController` + nav link **`/me`** (email, roles, id, `createdAt`) |

### IAM2: Admin platform (API + SPA)

Single code **IAM2** = **wave 2 (v1.2)**: backend admin endpoints **and** Angular admin area together.

| Deliverable | Status |
|-------------|--------|
| REST **`/api/v1/admin/users`** (list, get, create, set enabled, set roles) | **Done** |
| REST **`/api/v1/admin/roles`** (list) | **Done** |
| **`GET /api/v1/admin/audit-logs`** (paged) + **`/admin/audit-log`** UI | **Done** |
| **`SUPER_ADMIN` only** on `/api/v1/admin/**` (`SecurityConfig`) | **Done** |
| **`APP_ADMIN`** on **`/api/v1/branches/**`** only (not on IAM admin) | **Done** (single-app scope) |
| **IAM audit** persistence (**V4** `iam_audit_log`) on user create / enable / roles | **Done** |
| Metrics for admin actions | **Done** (`app_iam_admin_action_total`, etc.) |
| **`@PreAuthorize` / method-level** on admin REST controllers | **Done** (`@EnableMethodSecurity` + class-level `hasRole('SUPER_ADMIN')`). |
| **APP_ADMIN “scoped” IAM** (per-application users/roles, `applications` table) | **Deferred** — **YAGNI** for a single deployment; revisit if multi-tenant support is needed. |
| **`/admin`** lazy routes; **`roleGuard(['SUPER_ADMIN'])`**; shell link | **Done** |
| Users / roles / audit screens; TanStack Query (**F3** pattern) | **Done** |
| Create-user flow + password policy checklist | **Done** |
| Login/register UX for **email + phone + Google** (§1.1) | **Partial** — **email/password login only**; **phone/Google** → **IAM4** |
| **Change-password** UI (and API) | **Gap** → typically **IAM3/4** with reset flow, or small dedicated story |
| **Postman / OpenAPI** kept in sync with admin endpoints | **Ongoing** — verify when APIs change |

**IAM2 / v1.2 closure:** Treat **v1.2** as **closed** when the **Done** rows above match your deployment. Rows **Partial** (phone/Google login) or **Gap** (change-password) are **not** part of the core IAM2 slice: they are either deferred to **IAM4** or optional polish. **APP_ADMIN-scoped IAM** is explicitly **YAGNI** here. To formalize closure: run [verification commands](#verification-quick) below, update Postman if needed, and tag or note **v1.2** in the changelog or PR.

### What to do next (suggested order)

1. **v1.1** is **closed** (tag **`v1.1.0`** — see [CHANGELOG.md](../../CHANGELOG.md)).
2. **v1.2 (IAM2) for this repo:** closed with **method security** + **audit log read** + admin UI; multi-app IAM remains an optional extension.
3. **IAM2 polish (optional):** **change-password** if you add the API; keep register/OAuth/phone aligned with **IAM4**.
4. Then **IAM3** (refresh rotation, rate limits, MFA, …) per [evolution.md](evolution.md).

### Progress note (recent delivery)

- **SPEC 03 — Frontend visual redesign** (last done item): "wayfinding" identity for the SPA, frontend only (no API change). DaisyUI `light`/`dark` overridden with brand tokens (route green, signal amber, paper/ink); self-hosted Overpass Variable; branching-node logo reused as the favicon; sidebar + topbar shell (persisted collapse under `branch-hub.pref.v1.sidebar`, drawer below `lg`); split-screen login with a route-map drawing; new **`/dashboard`** post-login home (live summary sentence, quick actions, SUPER_ADMIN activity timeline); card view below `sm` for the branches, users and audit tables; copy pass (sentence case, matching action and toast verbs, directive empty and error states) with a unit spec for the `en`/`es` key sets; `npm run test:e2e:screenshots` for design review. See [frontend/README.md](../../frontend/README.md#design-direction).
- **SPEC 02 — Flyway owns the schema**: added `spring-boot-starter-flyway`; one frozen baseline `V1__init_schema.sql`; `ddl-auto=validate` in every profile; Testcontainers 2.x with `@ServiceConnection` and one shared PostgreSQL for every Spring context test (no H2); `FlywayMigrationIntegrationTest`. Local stacks need one `docker compose down -v`. **Next: CI with GitHub Actions** (now unblocked).
- **SPEC 01 — rename to Branch Hub and publish readiness**: renamed the project to `branch-hub` across code (package `dev.jotaeme.branchhub`), database (`branchhub`), Compose, Prometheus, Grafana, SPA and Postman; MIT license; [architecture](../architecture.md) and [getting started](../getting-started.md) docs; roadmap rewritten as project docs. Fixed during its health pass: the Kafka consumer group never formed on the single Compose broker (internal-topic replication factor), the p95 and failed-notifications panels had no data, failed logins were not counted, and the admin tabs were unstyled (see `CHANGELOG.md`). Also found that Flyway never ran on Boot 4 (missing `spring-boot-starter-flyway`); fixed in **SPEC 02**.
- **v1.1 closure:** **`GET /api/v1/me`** (`MeController`); SPA **`/me`** + shell **Profile** link; Postman **1.05 Me**; TanStack `me` queries cleared on login/logout.
- **v1.2 closure:** `@EnableMethodSecurity` + `@PreAuthorize("hasRole('SUPER_ADMIN')")` on admin controllers; **`GET /api/v1/admin/audit-logs`** (paged); admin UI subnav + **Audit** tab with TanStack Query.
- **IAM2** advanced with reusable validation utilities:
  - Backend shared password policy utility wired into admin user creation.
  - Frontend reusable account validation utility + visual password rules checklist in Admin create-user flow.
- List endpoints: default `size` aligned to **10** in `PageRequestParams` (matches Admin UI / branches list).
- **F3 (wave 3):** TanStack Query — branches **and admin users/roles** use query keys + cached reads (`staleTime` 5 min, `refetchOnWindowFocus`) with invalidation on mutations.
- Lazy loading: `admin` and `branches` route-level `loadChildren`.
- Delivery tuning: DaisyUI `include` subset, `nginx.conf` gzip + long-cache hashed assets + no-store `index.html`, Angular `initial` budget (`600kB` warning since SPEC 03).

---

## B1 — Backend (recap)

OpenAPI, `PagedResponse`, Problem Details, branch CRUD + list endpoints, tests.

## B2 — Backend (recap)

JWT + HttpOnly refresh cookie, auth matrix, observability, Compose `app` / `web`.

## F1 — Frontend first slice (recap)

| Item | Notes |
|------|--------|
| Stack | Angular 21, Signals, Tailwind, DaisyUI |
| Shell + auth | Login, guards, `withCredentials` |
| Branches | Paged list + create |
| UX | Theme + locale prefs, responsive footer, toasts, route errors |
| Quality | Unit tests, Playwright smoke, `openapi-typescript` + `api-types` + `ApiPaths` |

## F2 — Frontend second slice (recap)

| Item | Notes |
|------|--------|
| Detail | `GET /api/v1/branches/{id}` — `BranchDetailComponent`, link from list |
| Update | `PUT /api/v1/branches/{id}` — `BranchEditComponent` (name/city; code read-only) |
| Deactivate | `PATCH .../deactivate` — in-app confirmation modal, hides action when inactive |
| Paths | `branchByIdPath` / `branchDeactivatePath` in `api-paths.ts` |
| E2E | Smoke extended: list → detail → edit → deactivate |

## F2+ — UI polish (recap)

Shared loading, shell (account menu, tab title, favicon), footer — see [frontend/README.md](../../frontend/README.md) (*Reusable loading*, *Branding & shell UX*).

## H1 — Hardening (refresh + headers)

| Item | Notes |
|------|--------|
| Refresh interceptor | `core/interceptors/refresh.interceptor.ts`: on `401`, one `POST /api/v1/auth/refresh` via `HttpBackend` (no interceptor loop), single-flight, retry original request with `X-Retry-After-Refresh`; order with `apiInterceptor` / `authErrorInterceptor` documented in `app.config.ts` |
| CSP & headers | `frontend/nginx.conf`: `Content-Security-Policy`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy` |
| FOUC + CSP | Inline theme script moved to `public/theme-init.js` so `script-src 'self'` works under CSP (`index.html` loads it) |

`ng serve` does **not** apply Nginx headers; CSP applies to the **`web`** Docker image and similar deployments.

## Verification (quick)

```bash
# Backend (from repo root)
cd backend && ./gradlew clean test

# Frontend
cd frontend && npm ci && npm run build && npm test

# E2E smoke against the Compose stack (from frontend/; see docs/getting-started.md)
npm run test:e2e:smoke && npm run test:e2e:admin-smoke

# Design review screenshots (390/1280 px × light/dark, into frontend/e2e/__screenshots__/)
npm run test:e2e:screenshots
```

## Git workflow

See [overview.md §9.1](overview.md#91-mandatory-delivery-format-per-phase): branch, commit, push are executed manually.

---

[← Frontend roadmap](frontend.md) · [Overview →](overview.md)
