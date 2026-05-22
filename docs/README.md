# Documentation

Central index for **roadmap**, **API tooling**, and cross-cutting planning.

**Handoff:** Implementation detail (pagination, loading, i18n, etc.) lives in **`backend/README.md`** and **`frontend/README.md`**. You do not need a fresh chat window if you read those; roadmap files only summarize phases and status.

## Roadmap

| Document | Contents |
|----------|----------|
| [Overview & standards](roadmap/overview.md) | Principles, monorepo layout, cross-phase docs, cadence, decided standards, Git workflow |
| [**Base evolution (waves & v1.x)**](roadmap/evolution.md) | **Single ordered plan** — backend + frontend by wave; version bumps of the template |
| [**Delivery status (checklist)**](roadmap/status.md) | **v1.0** baseline (**B1, B2, F1, F2, H1**) **Done**; **F3** **Done**; **v1.1 (IAM1)** **Done** (incl. **`/me`**); **v1.2 (IAM2)** admin platform **Done**; **v1.4 (IAM3)** **Done** (backend + SPA **`/me/security`**, login MFA) — [IAM3 table](roadmap/status.md#iam3-operational-hardening-v14); [evolution.md](roadmap/evolution.md); verification commands |
| [Backend phases](roadmap/backend.md) | Phase 1 (API maturity), Phase 2 (security & reliability) |
| [Frontend phase](roadmap/frontend.md) | Angular stack, slices, folder rules, DoD, **F3** client query cache |
| [**Security (backend vs frontend, by phase)**](security.md) | i18n vs ngx-translate, production builds, obfuscation myths, hardening checklist |
| [**Identity & access (IAM1–IAM6)**](roadmap/auth-platform.md) | **§1.1** identity; **§2.2** priorities; **§2.3** ops/email/OAuth/compliance; phases IAM1–IAM6 |

## API contract tooling

- **Postman:** import [`postman/spring-web.postman_collection.json`](postman/spring-web.postman_collection.json) (collection lives with docs so backend and frontend share one obvious contract reference). Includes login (token or MFA challenge), refresh, logout, MFA verify/enroll, **refresh sessions (1.07–1.09)**, branches, and actuator samples.
- **OpenAPI 3:** served by the backend when enabled (e.g. `GET /v3/api-docs`, Swagger UI `/swagger-ui.html` on direct API port **8081** in local compose). The document registers **`bearerAuth`**; only operations annotated with `@SecurityRequirement` require the Authorize token in Swagger UI (login, refresh, logout, and MFA verify remain public).

## Language

Roadmap and technical docs in this repo are maintained in **English** (see [overview](roadmap/overview.md) — aligns with code and commit messages).
