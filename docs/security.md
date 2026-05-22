# Security guide — learning by layer (backend vs frontend)

This document explains **what “security” means** in a full-stack SPA + API setup, how it maps to **project phases**, and what is **already in place** vs **common misconceptions** (minification, obfuscation).

For delivery status, see [roadmap/status.md](roadmap/status.md).

---

## 1) Where the project stands (so you do not mix phases)

| Layer | Phase (roadmap) | Status for the *current* product goal |
|-------|-----------------|----------------------------------------|
| **Backend** | **B1** + **B2** (API maturity + auth/ops) | **Done** for branch API + JWT/cookie auth. **IAM** persistence and admin APIs: see [status.md — IAM](roadmap/status.md#iam-implementation-and-gaps-this-repo). |
| **Frontend** | **F1** (first slice) | **Done**: login, paged list, create branch, i18n, OpenAPI typings, tests, smoke. |
| **Frontend** | **F2** (second slice) | **Done**: branch detail, update, deactivate in the UI (API from **B1**). |
| **Frontend** | **H1** (hardening) | **Done**: `POST /api/v1/auth/refresh` via **`refreshInterceptor`** (cookie + `HttpBackend`); **CSP** and related headers on **`frontend/nginx.conf`**; FOUC script in **`public/theme-init.js`** for strict `script-src 'self'`. |
| **Full stack** | **IAM1–IAM2** (identity + admin platform) | **Largely done**; **`GET /api/v1/me`** + SPA **`/me`** shipped; other gaps (self-service OAuth, change-password, …) are listed in [status.md](roadmap/status.md). |

**Branch CRUD** in the browser is complete with **F2**. Further security work is **IAM waves** (audit, rate limits, MFA, recovery, OAuth, …) per [evolution.md](roadmap/evolution.md).

---

## 2) Internationalization — do we use Angular Translate / ngx-translate?

**No.** This repo does **not** use:

- **`@ngx-translate/core`** (third-party runtime i18n), or  
- **Angular’s built-in compile-time i18n** (`$localize`, translation XLF files, `@angular/localize`).

Instead, the SPA uses a small in-house **`I18nService`** (`frontend/src/app/core/i18n/i18n.service.ts`) with static dictionaries for **`en`** and **`es`**, plus persistence of the chosen locale via **`UserPreferencesService`**.

**Trade-offs (learning note):**

| Approach | Good for | This repo |
|----------|----------|-----------|
| Custom service + dictionaries | Small, fixed set of strings; full control; no extra deps | **Chosen** |
| `@ngx-translate` | Large apps, lazy-loaded JSON, dynamic language packs | Not used |
| Angular `$localize` | Best for build-per-locale, SEO, strict message extraction | Not used |

---

## 3) Production frontend builds — minification and bundles

**Minification and bundling are already enabled** for production.

- `npm run build` uses the **production** configuration by default (`angular.json`: `defaultConfiguration: "production"`).
- That turns on **optimization** (including **script minification** and **style minification**), **output hashing** for cache busting, and replaces `environment.ts` with `environment.prod.ts`.

So you do **not** need a separate “minify step”: the Angular CLI handles it.

**What minification is *not*:** it does **not** hide business logic or secrets. It only reduces size and renames local symbols inside bundles. Anyone can still read and debug minified JavaScript in the browser.

---

## 4) “Obfuscate / encode the code to keep it secret”

**Frontend code is always public** to the user’s browser. **Obfuscation** (tools that mangle names and control flow) is **not** a security control:

- It may slow down casual reading; it does **not** stop a determined reviewer.
- **Secrets must never live in the SPA** (API keys, JWT signing keys, DB passwords). If it ships to the browser, assume it can be extracted.

**Real controls** are on the **server** and **transport**: HTTPS, authentication, authorization, input validation, rate limits, secure cookies, CSP headers, etc.

---

## 5) Security topics by phase — **backend**

Aligned with [roadmap/backend.md](roadmap/backend.md).

### Backend Phase 1 (API maturity)

| Topic | Why it matters |
|-------|----------------|
| **Consistent errors (e.g. Problem Details)** | Clients and operators can handle failures without leaking stack traces inappropriately. |
| **Validation on input** | Never trust the network; reject bad data at the boundary. |
| **OpenAPI in sync with behavior** | Reduces “surprise” integrations and security gaps from undocumented endpoints. |
| **Pagination / limits** | Reduces abuse and accidental overload (`max size` on lists). |

### Backend Phase 2 (security & reliability)

| Topic | Why it matters |
|-------|----------------|
| **JWT access token (short-lived) in response body** | SPA keeps it in **memory** (not `localStorage` as the default pattern here). |
| **Refresh token in HttpOnly cookie** | Reduces XSS exfiltration compared to storing refresh tokens in JS-accessible storage. |
| **`Secure` / `SameSite` cookie flags** (as appropriate) | Mitigates cookie theft and CSRF in combination with same-site or CSRF strategies. |
| **Authorization matrix** | Ensures only allowed roles hit `/api/v1/branches/**`. |
| **Secrets via env / config** | `JWT_SECRET`, `APP_SUPER_ADMIN_PASSWORD`, etc. — never committed. |
| **Observability & alerts** | Detect attacks and failures (abuse patterns, dependency outages). |

**IAM hardening in this template (recent):**

- **Transactional email:** enabling a previously disabled user (`enabled: false` → `true`) enqueues **`ACCOUNT_ACTIVATED`** (see `backend/README.md` — IAM email outbox).
- **MFA (TOTP):** **SUPER_ADMIN** and **APP_ADMIN** may enroll TOTP; login then returns `mfaChallengeToken` until `POST /api/v1/auth/mfa/verify`. Secrets are encrypted at rest (`IAM_MFA_ENCRYPTION_KEY`). Details: [backend/README.md](../backend/README.md).

*Concrete behavior for this repo:* see [backend/README.md](../backend/README.md) (security matrix, auth endpoints).

---

## 6) Security topics by phase — **frontend**

Aligned with [roadmap/frontend.md](roadmap/frontend.md).

### Frontend **F1** — first slice (done)

| Topic | Why it matters |
|-------|----------------|
| **HTTPS in real deployments** | Stops token and cookie interception on the network. |
| **`withCredentials` + same-origin proxy in dev** | Cookies behave consistently with how the browser scopes them. |
| **Access token in memory only** | Reduces persistence window for XSS (still: XSS is a serious problem; avoid untrusted HTML). |
| **Route guards** | Prevents unauthenticated navigation to protected screens. |
| **Central HTTP errors / user feedback** | Avoids silent failures and encourages consistent handling of `401` / validation errors. |
| **No secrets in `environment.prod.ts`** | Only **public** config (e.g. API base URL). |

### Frontend **F2** (done) and beyond

| Topic | Why it matters |
|-------|----------------|
| **Authorize actions in UI** | Hide/disable buttons the user cannot perform — **but** the backend must still enforce (UI is not security). |
| **Confirm destructive actions** (e.g. deactivate) | **F2**: DaisyUI **modal** + `PATCH .../deactivate` (no native `window.confirm`); backend remains authoritative. |
| **`POST /api/v1/auth/refresh` (silent renewal)** | **H1**: `refreshInterceptor` retries the failed request once after a new access token; uses **`HttpBackend`** so refresh does not recurse; **`authErrorInterceptor`** still handles final `401` (logout + toast). |
| **Content Security Policy (CSP)** | **H1**: Nginx `web` sends CSP + `nosniff` + `Referrer-Policy` + `Permissions-Policy`; **`style-src 'unsafe-inline'`** kept for Tailwind/Daisy patterns — tighten over time if you move to stricter styling. |

---

## 7) Cross-cutting production checklist (high level)

| Area | Action |
|------|--------|
| **Transport** | TLS everywhere; HSTS where applicable. |
| **Cookies** | `Secure`, `HttpOnly`, sensible `SameSite`; correct path/domain. |
| **Headers** | CSP, `X-Content-Type-Options`, `Referrer-Policy`, etc. (often on **Nginx** `web` service). |
| **CORS** | In Docker, UI hits same origin via proxy; direct API access is for tools — keep rules explicit if you expose API to other origins. |
| **Dependencies** | `npm audit`, Gradle dependency check; patch regularly. |
| **Rate limiting / WAF** | For public APIs, consider gateway limits (future hardening). |

---

## 8) What to learn next (suggested order)

1. Tighten **CSP** further (hashes/nonce, drop `'unsafe-inline'` for styles if you can).  
2. **HSTS**, **COOP** / **COEP** (only with full audit — can break third-party scripts).  
3. **New backend or domain features** — new row in [status.md](roadmap/status.md) + threat model.

---

[← Documentation index](README.md) · [Roadmap overview](roadmap/overview.md) · [Status](roadmap/status.md)
