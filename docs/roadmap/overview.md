# Roadmap — Overview

This document defines the execution plan for evolving the project in structured phases.
All implementation, code comments, commit messages, and documentation should be written in English.

## 1) Guiding Principles

- DRY: avoid duplication in logic, contracts, and configuration.
- YAGNI: implement only what is required for the current learning goal.
- KISS: prefer simple designs over clever complexity.
- SOLID: keep responsibilities isolated and extensible.
- Clean Architecture: preserve boundaries between domain, application, infrastructure, and entrypoints.
- Best Practices: tests, observability, security, clear docs, and reproducible environments.

## 2) Scope Strategy (numbered milestones + base versions)

**Shipped sequence (v1.0):** **B1** → **B2** → **F1** → **F2** → **H1** (see [status.md](status.md)).

| Code | Meaning |
|------|---------|
| **B1** | Backend “Phase 1” in [backend.md](backend.md) — API maturity |
| **B2** | Backend “Phase 2” — security & operations |
| **F1** | Frontend **first slice** — login + branch list + create (+ prefs, tests, OpenAPI typings) |
| **F2** | Frontend **second slice** — branch detail + update + deactivate in the UI |
| **H1** | Frontend **hardening** — refresh interceptor, Nginx CSP / security headers |

**Future work:** do **not** tackle everything at once. Use [**evolution.md**](roadmap/evolution.md) — **waves**, **v1.1–v1.7**, backend + frontend slices per wave — then update [status.md](status.md) when a wave closes.

This sequencing reduces risk and prevents UI work from masking API design issues.

### 2.1) Repository layout (monorepo)

We use a **single repository** for backend and frontend. Boundaries stay strict: no cross-imports between Java and Angular; the contract is the HTTP API (OpenAPI).

```text
spring-web/
  backend/                # Spring Boot (Gradle)
    src/
  frontend/               # Angular app
  docs/
    roadmap/              # Phased plan (this folder)
    postman/              # Postman collection (API contract)
  monitoring/
  docker-compose.yml
  README.md               # Hub: links to backend/, frontend/, docs/
```

**Why monorepo:** one clone, aligned API versions, simpler CI (backend tests + frontend tests in one pipeline), ideal for a learning portfolio.

---

## 6) Cross-Phase Documentation Requirements

- Keep module READMEs updated at each phase close (`backend/README.md`, `frontend/README.md`).
- Add short architecture decision records for key decisions (optional under `docs/`).
- Provide verification commands for local validation.
- Keep change logs concise and focused on "why".
- For every phase, document in simple language:
  - What was implemented.
  - Why it was implemented (learning and engineering rationale).
  - How to verify it quickly.
- Every phase must include a **Git execution section**:
  - The assistant proposes exact Git commands.
  - The user runs those commands manually.
  - No hidden Git actions should be assumed.

---

## 7) Suggested Execution Cadence

- Work in small vertical slices.
- One feature = code + tests + docs + verification.
- Avoid large "big bang" merges.
- Suggested branch naming (align with **B1 / B2 / F1 / F2**):
  - `feat/b1-*`, `feat/b2-*`, `feat/f1-*`, `feat/f2-*` (or legacy `feat/backend-phase1-*`, `feat/frontend-*`)

---

## 8) Decided standards (baseline for implementation)

| Topic | Decision |
|-------|----------|
| API errors | **RFC 7807 Problem Details** as the target consistent format in Backend Phase 1 |
| SPA auth | **Access token short-lived in memory** + **refresh in HttpOnly cookie** (implemented in Backend Phase 2; document interim if needed) |
| First UI slice | **Login + list + create** only; extend in a second slice |
| DaisyUI themes | **`light` + `dark`** |
| Frontend tests | **Unit tests** + **one E2E smoke** |
| Repository | **Monorepo**; Angular under **`frontend/`** |
| Language | **English** for code, comments, commits, and user-facing docs |

---

## 9) Immediate next step

**v1.0** is complete (**B1, B2, F1, F2, H1**). **F3** is also complete. **v1.1 / IAM1** is **closed** for this template (identity, lockout, **`GET /api/v1/me`**, SPA **`/me`**). **v1.2 / IAM2** (admin API + admin UI) is **closed** in [**status.md**](status.md). **v1.4 / IAM3** is **done** in this repo for **backend and SPA template scope**: refresh rotation, sessions API, rate limits, MFA TOTP, email outbox, **`/me/security`**, and **login MFA**. Deferred items (phone/OAuth, registration, full security-email matrix, …) are listed in [**status.md — IAM**](status.md#iam-implementation-and-gaps-this-repo).

**Suggested focus:** **IAM4 / v1.5** (recovery, OAuth, phone) per [**evolution.md**](evolution.md), or optional polish (e.g. admin users table **MFA** column). Tag **`v1.4.0`** when you want to freeze the template milestone (see root `CHANGELOG.md`).

References:

1. [**status.md**](status.md) — **checklist**, IAM done vs gaps, verification commands.
2. [**evolution.md**](evolution.md) — **waves**, **v1.x** template versions, BE+FE ordering.
3. [auth-platform.md](auth-platform.md) — IAM design (**§1.1**, **§2.2**, **§2.3**).
4. [frontend.md](frontend.md) — **F3** client cache (closed).
5. **Stricter CSP** — [security.md](../security.md) (cross-cutting).
6. **New domain features** — add a row to [status.md](status.md) and assign a **wave** in evolution.md.

### 9.1) Mandatory delivery format per phase

At the end of each phase, produce:

1. **Phase Summary (plain English)** — What changed; why it matters; what was deferred (YAGNI).
2. **Verification Guide** — Exact commands; expected output/result.
3. **Git Commands (manual execution by user)** — Branch, add, commit, optional push.

Example:

```text
git checkout main
git pull
git checkout -b feat/backend-phase1-problem-details
git add <files>
git commit -m "feat(api): standardize problem details across endpoints"
git push -u origin feat/backend-phase1-problem-details
```

---

**Next:** [Evolution (v1.x waves)](roadmap/evolution.md) · [Backend phases](backend.md) · [Frontend phase](frontend.md) · [Status](roadmap/status.md)
