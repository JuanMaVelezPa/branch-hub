# Roadmap — Identity & access (multi-application IAM)

**Goal:** Evolve from the current **single in-memory `ADMIN`** (B2) to a **maintainable, reusable IAM layer** you can carry across future backends and frontends: **database-backed users**, **explicit roles**, **least privilege**, and **operator-friendly administration**.

**Implementation order:** use [**evolution.md**](evolution.md) — IAM work is split across **v1.1–v1.7** waves; this file is the **design reference**, not the schedule by itself.

**Current baseline (this repo):** JWT access token + HttpOnly refresh cookie; **PostgreSQL-backed** users and roles (`iam_user`, `iam_role`, Flyway **V3**); `UserDetailsService` is **`DbUserDetailsService`**; JWT **`sub`** is the user UUID; branch APIs require **`SUPER_ADMIN` or `APP_ADMIN`**; **`/api/v1/admin/**`** is **`SUPER_ADMIN` only**. See `SecurityConfig`, `AuthController`, `backend/README.md` (security matrix), [security.md](../security.md).

**Implementation snapshot vs waves:** the design codes **IAM1–IAM6** are still the long-term map, but several slices landed **ahead of the original wave order** (e.g. admin UI + **F3** query cache while IAM1 was still being finalized). Authoritative checklist: [status.md — IAM implementation and gaps](status.md#iam-implementation-and-gaps-this-repo).

**Pragmatic default:** Prefer **free or low-cost** building blocks first (SMTP modesto, SMS solo cuando haga falta, OAuth con cuotas gratuitas donde aplique). Upgrade paths (**paid** email/SMS, WAF, IdP, support tooling) are **explicitly deferred** until usage or risk justify the cost — avoid over-analysis up front; evolve with traction.

---

## 1) Role model (target)

Keep the model **small and explainable** (KISS). Extend later with permissions if needed.

| Role | Scope | Typical capabilities |
|------|--------|----------------------|
| **SUPER_ADMIN** | Platform | Full access across all applications and IAM APIs (create/disable users, assign roles, audit). **Break-glass** account(s); protect with MFA in production. |
| **APP_ADMIN** | One “application” / product | Manage users and settings **within that app** only (e.g. branches for this service). |
| **USER** | Application | Normal business operations (no IAM admin APIs). |
| **VIEWER** (optional) | Application | Read-only where the domain allows it. |

**Applications:** Represent each deployable product (or tenant) as a row (e.g. `applications` table) **or** as a fixed enum in v1 if you truly have one app — prefer a **table** if you already know several UIs will share the stack.

**Authorization rule:** Prefer **server-side checks** (`@PreAuthorize`, custom `PermissionEvaluator`, or policy objects in the application layer). The SPA only **hides** UI; it does not enforce security.

---

## 1.1) Identity anchors & account uniqueness *(decided direction)*

IAM for this stack is designed **with modern B2C-style accounts in mind**: people **sign in and verify** using **email**, **phone**, and/or **Google (OAuth)** — not a legacy “username string” as the main key. That keeps accounts **unique, recoverable, and easy to reason about** as the project grows.

| Concept | Rule |
|---------|------|
| **Stable internal id** | Every account has an immutable **`users.id`** (e.g. UUID) used in JWT `sub`, foreign keys, and audit — never rely on email/phone as the only PK. |
| **Primary login identifiers** | **Verified email** (normalized, case-insensitive uniqueness), **verified phone** (**E.164** uniqueness when set), and/or **OAuth** (`provider` + **`subject`** from Google, unique per provider). A user may have **more than one** link (e.g. email + Google) after linking flows. |
| **Password-based login** | Tied to a **verified** email or phone (same OTP/verification subsystem as IAM4); avoid a separate arbitrary “login name” unless a product requirement appears later. |
| **Alias / display handle** | Optional **`display_name`** or **`handle`** for UI (e.g. @nickname). If present, enforce **uniqueness** in the chosen scope (global per deployment for v1). It is **not** the security identifier — email/phone/OAuth remain the anchors for verification and uniqueness of the *person*. |
| **Uniqueness in DB** | **Unique** constraints on: normalized `email` (nullable allowed until verified, or use a registration state), `phone_e164` (partial unique where not null), `(oauth_provider, oauth_subject)`. Application rules: cannot complete registration without at least one verified anchor. |

**Why this matters:** Email and phone support **OTP**, resets, and anti-abuse; OAuth delegates proof to the provider. The optional alias satisfies “friendly name” without competing with security identifiers.

**Phase touchpoints:** **IAM1** introduces the schema and login paths for **email/password** (and seeds SUPER_ADMIN by email). **IAM4** adds **phone OTP**, **Google**, and links between methods on the same `users.id`.

---

## 2) Phased delivery (suggested codes)

Aligned with [status.md](status.md): implement in vertical slices (DB + API + tests + short docs each).

| Code | Scope | Outcomes |
|------|--------|----------|
| **IAM1** | **Persistence & login** | Flyway: `users` per **§1.1** (email/phone/OAuth fields, optional `handle`), `roles`, `user_roles`, optional `applications` / `user_application_roles`. **Shared password policy** (§2.1). Bcrypt storage. **Failed-login counters** + **lockout** (per **account id** and/or **identifier + IP** window). Replace `InMemoryUserDetailsManager` with **DB-backed** `UserDetailsService`. JWT: **`sub` = user id** + **roles** (+ `aud` / `app_id` if multi-app). Seed **SUPER_ADMIN** (email from env). |
| **IAM2** | **Admin platform (API + SPA)** | REST under `/api/v1/admin/...`: users, roles, optional **audit read**; Spring Security + **`@PreAuthorize`** where useful; **audit** table for IAM mutations; **password policy** on admin create. **Angular `/admin`**: role guards, users/roles/audit screens, OpenAPI typings. *Legacy note:* older drafts split this into “IAM2 backend” + “IAM3 frontend”; this repo uses **one code IAM2** for **v1.2**. Extended login (phone/Google/register) stays **IAM4**-heavy. |
| **IAM3** | **Hardening & trust operations** | Refresh token **rotation**; **session / device list** + revoke others; **rate limits** on login, register, OTP endpoints; **TOTP MFA** for **SUPER_ADMIN** / optional **APP_ADMIN**; **security notification emails** (new login, password change); CSP follow-ups. See **§2.2 (P2)**. *Legacy:* older drafts called this **IAM4**. |
| **IAM4** | **Recovery & alternate sign-in** | **Forgot password** + **OTP-backed reset** (email → SMS). **Google** (OAuth2 / OIDC). **Phone** verified + SMS OTP login path. See §2.1. *Legacy:* older drafts called this **IAM5**. |
| **IAM5** (optional) | **Account lifecycle & step-up** | **Mandatory verification** UX (block or limit until email/phone verified — align §1.1); **step-up re-auth** (password / OTP / MFA) for change email, change phone, **delete account**; **export my data** + **delete account** (GDPR-style); **consent / ToS version** stored at signup; **CAPTCHA** on hot endpoints when abuse appears. See **§2.2 (P3)**. *Legacy:* older drafts called this **IAM6**. |
| **IAM6** (optional) | **Advanced credentials & integrations** | **Passkeys / WebAuthn** (passwordless, phishing-resistant); **API keys** or OAuth2 client credentials for **machine-to-machine** (only if integrations need them). See **§2.2 (P4–P5)**. *Legacy:* older drafts called this **IAM7**. |

**YAGNI:** Do not build a full generic policy engine until two real apps need conflicting rules.

---

## 2.1) Password policy, lockout, recovery & social login *(review backlog)*

Single organization per deployment stays the default; features below are **reusable utilities** across all password-based flows.

### Shared password validation (backend + frontend)

| Layer | Responsibility |
|-------|------------------|
| **Backend** | One **password policy** component (domain or application module): min/max length, character classes, deny common passwords (optional list), normalize Unicode. Used by **register**, **change password**, **admin set password**, **reset password**. Expose rules via **Problem Details** / field errors with stable codes. |
| **Frontend** | Mirror the same rules with Angular **validators** + optional strength hint — **one documented mapping** from API error codes to messages (avoid divergent copy). Prefer importing **constants** generated from OpenAPI or a tiny shared JSON contract so DRY holds between backend and frontend. |

**Progress note (implemented early):** IAM2 admin user creation now uses a shared password policy utility in backend and a reusable visual validation helper in frontend (real-time criteria checklist in the create-user modal).

### Login retries and lockout

- Persist **failed attempt count** and optional **`locked_until`** (or permanent lock + admin unlock).
- Optional: track by **login identifier (email / phone / OAuth attempt key) + client IP** to slow brute force without punishing shared NAT users too harshly.
- Return **generic** error on failed login (“invalid credentials”) where possible; log details server-side only.
- Clear counter on successful login.

### Forgot password and reset with OTP

- **Flow:** `forgot` → issue **time-limited OTP** (store **hash** of OTP + expiry, single-use) → user submits OTP + new password (must pass **password policy**) → invalidate OTP and sessions as needed.
- **Channels:** start with **email OTP** (SMTP or transactional provider). **SMS OTP** later (provider API keys, cost, regional rules).
- **Rate limit** OTP requests per email/phone/IP; cooldown between sends.
- **Security:** short TTL (e.g. 10–15 min), constant-time compare on OTP verification, no user enumeration if feasible (same response shape).

### Sign-in with Google and phone (UX)

| Method | Approach | Notes |
|--------|-----------|--------|
| **Google** | Spring **OAuth2 Login** / OIDC; map verified email or `sub` to `users` (link or create). Each deployment registers its own **Google Cloud OAuth client** per environment. |
| **Phone** | Treat phone as **E.164** + **verified** flag; primary flow often **SMS OTP** (same OTP subsystem as password reset). Builds “login with phone” UX without storing SMS as password. |

UI: clear buttons (“Continue with Google”), accessible phone input (country code), consistent with existing shell/DaisyUI — learning goal includes **polished auth screens**.

### Suggested ordering

1. **IAM1:** policy utility + lockout columns + tests.  
2. **IAM4 (can start after IAM2):** email OTP reset + Google OAuth in parallel tracks if capacity allows; SMS after email path is stable.

---

## 2.2) Prioritized backlog — modern auth patterns

Order below is **recommended execution priority** after the core path **IAM1 → IAM2** (identity + admin platform). Items overlap phases; use this table when splitting work.

| Priority | Topic | Goal | Typical phase |
|----------|--------|------|----------------|
| **P1** | **Verification-first access** | Restrict features until **email and/or phone** is verified (§1.1); clear UX for “resend verification”. | **IAM1 / IAM2** (rules + admin/shell UI); tighten defaults in **IAM5** if deferred. |
| **P1** | **Rate limiting on auth surfaces** | Throttle **login**, **register**, **forgot-password**, **OTP send** per IP / identifier to slow abuse. | **IAM3** (infra: gateway or Spring filters + Redis or bucket store). |
| **P2** | **Security notification emails** | Notify on **new device / new location login**, **password changed**, optional **MFA enabled**. | **IAM3** (templates + async mail). |
| **P2** | **Session & device management** | List refresh sessions / devices; **logout others**; show last activity. | **IAM3** (persist refresh/session metadata). |
| **P2** | **TOTP MFA** | Authenticator app for **SUPER_ADMIN** (required in prod) and optional for users. | **IAM3** (secrets encrypted at rest). |
| **P2** | **Step-up authentication** | Require recent **password**, **OTP**, or **MFA** before **change email**, **change phone**, **delete account**, sensitive settings. | **IAM5** (policy + endpoints). |
| **P3** | **CAPTCHA** | Add **after** rate limits when bots persist (register, forgot password). | **IAM5** or when metrics justify (hCaptcha / Turnstile). |
| **P3** | **Account export & deletion** | **Export** user-related data; **soft/hard delete** with audit; align legal copy per deployment. | **IAM5** |
| **P3** | **Consent & policy versioning** | Store **ToS / privacy version accepted** at registration; re-prompt on material updates when needed. | **IAM2 / IAM5** |
| **P4** | **Passkeys (WebAuthn)** | Passwordless sign-in; strong phishing resistance; optional second factor. | **IAM6** |
| **P5** | **API keys / M2M** | Separate from human JWT; scopes; rotation — only for **integrations** or worker services. | **IAM6** |

**Note:** **P1 verification** is assumed in product rules from **IAM1** onward (schema has `*_verified_at`); **enforcement** in the app can ship incrementally so the first vertical slice still works.

---

## 2.3) Additional considerations *(operations, delivery, trust)*

Items below complement **§2.2** — not all belong in code; several are **per-deployment** policy.

| Area | What to keep in mind |
|------|----------------------|
| **Observability** | Metrics for **login success/failure**, OTP send rate, lockouts (aggregated — avoid PII in labels). Structured logs with **user id** and **correlation id**; never log passwords, OTPs, or raw tokens. Dashboards/alerts for auth anomalies. |
| **Email deliverability** | For OTP and security mails: **SPF, DKIM, DMARC** on the sending domain; use a reputable provider; handle bounces so you do not keep dead addresses as verified. |
| **OAuth redirects** | **Whitelist** redirect URIs per environment (dev/staging/prod); misconfiguration is a common vulnerability. Document the exact URLs per environment. |
| **Time & tokens** | Servers on **accurate time** (NTP); JWT validation allows small **clock skew**; **TOTP** clients need correct device time. |
| **i18n & a11y** | All auth copy (errors, verification, MFA) via your **i18n** layer; forms **keyboard**-navigable, errors linked to fields (**WCAG** basics). |
| **Backups & DR** | `users` / sessions / audit are **critical** — include in backup/restore drills; define RPO/RTO for identity data per deployment. |
| **Compliance & copy** | Privacy policy / consent text **per deployment** and jurisdiction; if you store EU users, plan **GDPR**-aligned export/delete (IAM5); B2B may need **DPA** templates. |
| **CI / supply chain** | **Secret scanning** on commits; `npm audit` / Gradle dependency check in pipeline; pin or review major auth-related library upgrades. |
| **Incident playbooks** | How to **disable** a compromised account globally, **rotate** `JWT_SECRET`, notify users (template), post-mortem doc. |
| **Support & abuse** | Path for **manual unlock** (admin) vs self-service; process for **reported account takeover**; optional **IP allowlists** only for high-risk admin UIs. |
| **Mobile / deep links** | If native apps ship: **universal links** / app links for email verification and OAuth return URLs — plan early to avoid brittle WebView hacks. |

---

## 3) Database (high level)

- **Normalized** users and roles; **no** role names duplicated as strings in business tables without a foreign key to `roles`.
- **`users` identity (§1.1):** immutable **`id`** (PK); **`email`** normalized (unique where not null); **`phone_e164`** (unique where not null); **`email_verified_at`**, **`phone_verified_at`**; **`oauth_provider`** + **`oauth_subject`** (unique composite per provider, nullable); optional **`handle`** or **`display_name`** with unique constraint if used.
- **Indexes / constraints** on the columns above; foreign keys for `user_roles`.
- **Secrets:** never store plaintext passwords; use BCrypt (cost factor reviewed periodically). Password row may be absent for **OAuth-only** accounts until a password is set.
- **Lockout:** `failed_login_count`, `locked_until` (or equivalent) on `users` (or separate table keyed by `user_id`).
- **OTP / reset:** separate table or columns for **hashed OTP**, **purpose** (reset vs verify-phone), **expires_at**, **consumed_at** — never store plaintext OTP.
- **Soft delete** or `enabled` flag on users; retain audit history.
- **Sessions / devices (IAM3):** store refresh token family, user-agent hash, IP, `last_seen_at` — design for **revocation** and “logout everywhere except this”.
- **TOTP (IAM3):** encrypted **totp_secret** or use a vault field; backup codes optional.
- **WebAuthn (IAM6):** credential table keyed by `user_id` + `credential_id`.
- **Consent (IAM5):** `terms_accepted_version`, `terms_accepted_at` on `users` or separate `user_consents` for audit trail.

---

## 4) Security practices (non-negotiable)

- **Least privilege:** default deny; explicit grants per endpoint and use case.
- **Secrets:** `JWT_SECRET` from env; rotate procedure documented; different keys per environment.
- **Transport:** HTTPS in production; `Secure` cookies when TLS is on (`security.cookies.secure`).
- **Tokens:** short access TTL; refresh rotation in IAM3; validate `iss`, `aud`, `exp` (already partially implied by current JWT setup — formalize in IAM1).
- **Input validation:** Bean Validation on all admin DTOs; idempotent safe retries where applicable.
- **Logging:** never log passwords or raw tokens; log user **id**, correlation id, action for IAM2+.
- **Dependency hygiene:** periodic `./gradlew dependencyUpdates` / `npm audit` in CI.

---

## 5) Documentation & reuse

- Update **`backend/README.md`** (auth matrix, admin routes) and **`frontend/README.md`** (admin shell) when **IAM2** ships or changes.
- Keep **Postman** collection in sync for admin endpoints.
- Single source for role names: **Java enums** + same strings in JWT factory + Angular constants generated or imported from OpenAPI.
- Identity fields and validation messages for **email / phone / handle** documented once; registration flows must enforce **§1.1** uniqueness rules in API tests.

---

## 6) Open questions (answer before IAM1 coding)

1. **Multi-tenancy:** One database with `tenant_id` on rows, or separate schemas per customer? (Affects JWT claims and queries.) *Default for this project: **single org per deployment**; defer multi-tenant SaaS unless needed.*
2. **Identity provider:** Core stack stays **custom JWT** + **Google OAuth** (IAM4). Optional **enterprise IdP** (Keycloak, Entra) only if a deployment requires it.
3. **Super admin bootstrap:** Single seed user by **email + password** from env/migration (aligned with §1.1).
4. **Per-app admins:** Fixed set of applications in DB vs. one deployment per app with shared IAM service?

**Locked for design:** **§1.1** — accounts are anchored on **email / phone / OAuth**; optional **handle** for display; **internal user id** for all security and joins.

Document remaining decisions in a short ADR under `docs/` when you lock them.

---

## 7) Verification (when IAM phases ship)

```bash
cd backend && ./gradlew test
cd frontend && npm ci && npm run build && npm test
```

Add integration tests for: login → JWT with roles → forbidden without role → admin mutation audited → password policy rejection → lockout behavior → (IAM4) OTP reset and OAuth → (IAM3) MFA enrollment and session revoke → (IAM5) step-up before delete/export when implemented.

---

[← Status](status.md) · [Overview](overview.md) · [Security](../security.md)
