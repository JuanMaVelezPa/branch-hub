# Changelog

All notable changes to this template are documented here. Version labels **`v1.x`** follow [docs/roadmap/evolution.md](docs/roadmap/evolution.md) (template maturity), not npm or Gradle artifact versions.

## v1.1.0

**Closed:** IAM1 baseline for this repo (email/password identity, lockout, DB users) plus current-user profile for the SPA.

- **Backend:** `GET /api/v1/me` returns id, email, enabled, lock state, `createdAt`, roles (any authenticated user).
- **Frontend:** route `/me`, shell nav **Profile** / **Perfil**, TanStack Query + cache invalidation on login/logout.
- **Tooling:** Postman request **1.05 Me - Current profile** (after **1. Auth - Login**).
- **Docs:** `status.md` marks **v1.1** **Done**; roadmap index updated.

To record this release in Git (optional):

```bash
git tag -a v1.1.0 -m "Template v1.1: IAM1 + GET /me"
```

## v1.4.0

**Closed:** IAM3 operational hardening — refresh session persistence and rotation, auth rate limits, TOTP MFA for privileged roles, refresh-session management API, transactional email outbox with activation notification on user re-enable, and SPA **security** + **login MFA** flow.

- **Backend:** Flyway **V5** `iam_refresh_session`; **V6** `iam_email_outbox` + templates; **V7** MFA columns; **POST** `/api/v1/auth/refresh` with rotation; **GET/DELETE/POST** `/api/v1/auth/sessions` (+ `revoke-others`); **GET/POST** `/api/v1/auth/mfa/*`; login **MFA challenge** JWT when applicable; Bucket4j rate limits (Caffeine / optional Redis); **`mfaEnabled`** on **`GET /me`** and **`AdminUserResponse`**.
- **Frontend:** **`/me/security`** (sessions + admin TOTP enrollment), login MFA step, profile link + **`mfaEnabled`** display.
- **Tooling:** Postman **1.07–1.09** (list / revoke session / revoke others); MFA requests **1.03–1.05**; collection description documents cookie + MFA variables.
- **Docs:** `status.md` / `evolution.md` / `overview.md` / `auth-platform.md` baseline updated for **v1.4**; SPA security/MFA aligned with backend.

Optional Git tag:

```bash
git tag -a v1.4.0 -m "Template v1.4: IAM3 (sessions, MFA, rate limits, email outbox, SPA security)"
```
