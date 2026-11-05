# Roadmap — Evolution (waves & versions)

This document is the **single ordered plan** for growing the monorepo: **one wave at a time**, backend and frontend aligned where needed, **learning from each delivery** before scaling scope. Detailed specs stay in [auth-platform.md](auth-platform.md), [frontend.md](frontend.md), [backend.md](backend.md).

**Version label (`v1.x`):** the release number of this project — not the npm or Gradle artifact version. Bump **`v1.x`** when a **wave** closes (document it in the commit, the PR and [CHANGELOG.md](../../CHANGELOG.md)).

### Milestone codes

Waves use design codes, in this order: **IAM1** (wave 1) → **IAM2** (wave 2, admin API + admin UI) → **F3** (wave 3) → **IAM3** (wave 4) → **IAM4** (wave 5) → **IAM5** (wave 6) → **IAM6** (wave 7). The next **IAM** milestone after **v1.2** is **IAM3** / **v1.4**, unless waves 4 and 5 are swapped (see the note in wave 5).

**Rules**

- Finish **one wave** (tests + docs + verify) before starting the next unless a spike is explicitly time-boxed.
- Each wave has a **vertical slice** when possible: API + UI + contract (OpenAPI) + tests.
- **Defer** IAM3–IAM6 details until earlier waves are stable — see priority tables inside [auth-platform.md](auth-platform.md).

---

## Shipped baseline — **v1.0**

| Layer | Milestones | Notes |
|-------|------------|--------|
| Backend | **B1**, **B2** | API maturity, JWT + refresh cookie, Docker, metrics |
| Frontend | **F1**, **F2**, **H1** | Branches CRUD UI, refresh interceptor, CSP / Nginx headers |

Checklist: [status.md](status.md) (steps 1–5 **Done**).

---

## Wave 1 → **v1.1** — Identity persistence (backend-first)

| Track | Deliverables |
|-------|----------------|
| **Backend** | **IAM1** — DB users / roles (email/password baseline per §1.1 [auth-platform.md](auth-platform.md)), shared password policy, **lockout**, seed SUPER_ADMIN, JWT `sub` = user id, DB-backed `UserDetailsService`. |
| **Frontend** | Login (and any **email/password** flows) aligned with API; OpenAPI typings regenerated. **Profile:** **`GET /api/v1/me`** + SPA **`/me`** — see [status.md](status.md). |

**Original note:** “No full admin UI yet” applied to the **first** IAM wave; in this repo **wave 2** delivers **admin API + admin UI** under a single code **IAM2**. Treat wave 1 as **identity + login**; wave 2 as **admin platform (full stack)**.

**Outcome:** Real accounts in PostgreSQL, with the schema in Flyway migration scripts.  
**Learn:** End-to-end persistence auth, migration discipline.

**Phone / OAuth / handle columns:** deferred to **IAM4+** (and optional preparatory migrations) unless you need schema early — see [status.md](status.md).

---

## Wave 2 → **v1.2** — Admin platform (**IAM2**: API + SPA)

| Track | Deliverables |
|-------|----------------|
| **Full stack** | **IAM2** — Admin REST under `/api/v1/admin/...` (users, roles, audit read), Spring Security (+ optional **`@PreAuthorize`**), **IAM audit** on mutations, password policy on create; Angular **`/admin`** (guards, users/roles/audit UI, OpenAPI typings). Optional **APP_ADMIN**-scoped IAM for multi-app; this **single-app** project keeps IAM admin **SUPER_ADMIN-only**. |

**Outcome:** Operators manage identity from the browser (super-admin only).  
**Learn:** Authorization beyond a single generic admin, audit trail, one milestone name for the vertical slice.

**Reality check:** wave 2 is **closed** in this repo for the single-app scope: **method-level** `hasRole('SUPER_ADMIN')` on admin controllers, **paged audit read** (`GET /api/v1/admin/audit-logs`), and SPA **`/admin/audit-log`**. Only **multi-app / APP_ADMIN-scoped IAM** stays intentionally deferred — see [status.md — IAM2](status.md#iam2-admin-platform-api--spa).

---

## Wave 3 → **v1.3** — Client query cache (frontend-first)

| Track | Deliverables |
|-------|----------------|
| **Backend** | No required change (optional HTTP cache headers later). |
| **Frontend** | **F3** — TTL + invalidation on branch CRUD; reusable cache layer for future lists — see **F3** in [frontend.md](frontend.md). |

**Outcome:** Fewer redundant GETs; snappier navigation.  
**Learn:** Stale-while-revalidate, cache keys, invalidation rules.

**Note:** If **v1.3** is needed earlier for UX, you may schedule **F3** right after **v1.0** as a **small side wave** — only if IAM work is not blocking; default order above keeps **identity correct** before optimizing traffic.

**This repo:** **F3** was delivered **in parallel** with IAM work (admin lists use the same query-cache pattern as branches). That is fine; [status.md](status.md) tracks **wave closure** per milestone, not strict chronological order.

---

## Wave 4 → **v1.4** — Operational hardening

| Track | Deliverables |
|-------|----------------|
| **Backend** | **IAM3** — Refresh rotation, session/device metadata, rate limits, TOTP MFA (privileged), security notification emails. |
| **Frontend** | Sessions UI (optional), MFA enrollment, settings. |

**Outcome:** Stronger production posture; fewer abuse windows.  
**Learn:** Token families, MFA, transactional email in prod.

---

## Wave 5 → **v1.5** — Recovery & alternate sign-in

| Track | Deliverables |
|-------|----------------|
| **Backend** | **IAM4** — Forgot password, OTP (email → SMS), Google OAuth, phone verification paths. |
| **Frontend** | Screens for forgot/reset, OAuth buttons, phone OTP UX; i18n strings. |

**Outcome:** Modern login paths; aligns with §1.1 identity anchors.  
**Learn:** OTP lifecycle, OAuth redirect discipline, provider config per environment.

**Reorder note:** If you prefer **forgot password / Google** before **MFA / sessions**, swap **Wave 4** and **Wave 5** — document the choice in your PR when you start.

---

## Wave 6 → **v1.6** — Account lifecycle & trust

| Track | Deliverables |
|-------|----------------|
| **Backend + Frontend** | **IAM5** — Verification enforcement, step-up auth, export/delete, consent versioning, CAPTCHA if needed ([auth-platform.md](auth-platform.md) §2.2 P3). |

---

## Wave 7 → **v1.7** — Advanced credentials & integrations

| Track | Deliverables |
|-------|----------------|
| **Backend + Frontend** | **IAM6** — Passkeys (WebAuthn), API keys / M2M if integrations need them. |

---

## Cross-cutting (apply when relevant; do not block waves)

| Topic | Where |
|-------|--------|
| Stricter CSP, TLS | [security.md](../security.md), Nginx — incremental with any frontend wave |
| New domain features (non-IAM) | New rows in [status.md](status.md); same wave discipline |
| Free-tier pragmatism | [auth-platform.md](auth-platform.md) (baseline paragraph + §2.3) |

### Maintenance practices

These **do not replace** the versioned waves above; revisit them **between** waves or in CI so the project stays healthy without scope creep.

| Practice | Notes |
|----------|--------|
| **CI on every PR** | Run `./gradlew test` and `npm ci && npm run build && npm test` (and smoke E2E when feasible) so changes do not regress silently. |
| **Dependencies** | Use Dependabot/Renovate or a monthly review — priority: security patches for auth, Spring, Angular. |
| **API contract** | Keep OpenAPI and generated typings aligned; optional CI step fails if spec and code drift. |
| **Git tags + changelog** | Tag **`v1.x`** when a wave closes; one-line **CHANGELOG** entry that records what each release contains. |
| **Environments** | Same deploy shape for `dev` / `staging` / `prod`; only config/secrets differ (see root `.env.example`). |
| **Container bases** | Periodically bump base images (JDK, Node build stage, Nginx) for CVE fixes. |
| **License** | The project is MIT-licensed ([LICENSE](../../LICENSE)); keep the file and its copyright line. |
| **i18n & a11y** | New screens: strings via i18n; forms usable with keyboard — matches existing shell patterns. |
| **Frontend weight** | Prefer **lazy routes** and avoid unnecessary bundle growth as features land. |

---

## Summary table

| Version | Wave | Focus | Codes |
|---------|------|--------|--------|
| **v1.0** | — | Baseline shipped | B1, B2, F1, F2, H1 |
| **v1.1** | 1 | DB identity | IAM1 |
| **v1.2** | 2 | Admin platform (API + UI) | **IAM2** |
| **v1.3** | 3 | SPA cache | F3 |
| **v1.4** | 4 | MFA, sessions, rate limits | IAM3 |
| **v1.5** | 5 | Recovery + OAuth + phone | IAM4 |
| **v1.6** | 6 | Lifecycle, export, consent | IAM5 |
| **v1.7** | 7 | Passkeys, API keys | IAM6 |

---

[← Status](status.md) · [Overview](overview.md) · [Auth platform](auth-platform.md) · [Frontend](frontend.md)
