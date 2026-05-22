# Branches UI (Angular)

Angular SPA for the portfolio monorepo: **login**, **paged branch list**, **branch detail**, **create / edit**, and **deactivate**. It targets the backend contract under `/api/v1` and surfaces **Problem Details** messages when the API returns them.

## Stack

- Angular 21 (standalone components, lazy routes)
- Signals for local UI state
- **Tailwind CSS v4** with **PostCSS** (official Angular guide: [Install Tailwind CSS with Angular](https://tailwindcss.com/docs/installation/framework-guides/angular))
- **DaisyUI v5** as a Tailwind plugin ([install](https://daisyui.com/docs/install/), [themes](https://daisyui.com/docs/themes/))
- Vitest via `ng test`

### Tailwind + DaisyUI (how this repo is wired)

| Piece | Purpose |
|-------|---------|
| `.postcssrc.json` | Registers `@tailwindcss/postcss` exactly as in the Tailwind + Angular docs. |
| `src/styles.css` | `@import "tailwindcss" source(".")` limits scanning to `src/` (paths are relative to the stylesheet). |
| `@plugin "daisyui" { themes: … }` | Enables **light** (default) and **dark** (`prefers-color-scheme: dark`). Matches toggling `data-theme` on `<html>` in `index.html` and `app-shell`. |
| `angular.json` → `styles` | Includes `src/styles.css` so the CLI runs PostCSS on the global stylesheet. |

Production build turns off `inlineCritical` for styles so the main stylesheet is not deferred via the `media="print"` trick (avoids unstyled first paint in some browsers).

## Tooling versions (last reviewed)

These match the **latest stable** lines on npm for this stack (April 2026), except where noted:

| Package | Version | Notes |
|--------|---------|--------|
| `@angular/core` and Angular framework packages | **21.2.8** | Latest patch on the stable 21.x line. |
| `@angular/cli` / `@angular/build` | **21.2.7** | Latest CLI on npm; patch may trail framework slightly. |
| `tailwindcss` / `@tailwindcss/postcss` | **4.2.2** | Latest Tailwind v4 on npm. |
| `daisyui` | **5.5.19** | Latest v5 (Tailwind v4 plugin). |
| `typescript` | **~5.9.x** | Required by Angular 21 (`>=5.9 <6.1`); do not jump to TS 6 until Angular supports it. |
| `rxjs` | **~7.8.2** | Latest 7.8.x. |

Angular **22** exists as **prerelease** (`next`); this project stays on **21.x stable** for portfolio stability.

## Prerequisites

- Node.js 20+ (tested with Node 22)
- For local dev: API at `http://localhost:8080` when you run Spring alone (Gradle). With **Docker Compose**, use the SPA at **http://localhost:8080** (`web`) and the API **direct** at **http://localhost:8081** (`app`); the bundled UI still uses relative `/api` on :8080, which Nginx forwards to Spring.

## Install

```bash
cd frontend
npm ci
```

## Development

`ng serve` is configured with **`proxy.conf.json`**: browser calls go to `http://localhost:4200/api/...` and are forwarded to the Spring app, so **JWT stays in memory** and the **refresh cookie** can be set on the same host as the dev server.

```bash
npm start
```

Open `http://localhost:4200/`. Default credentials (if you copied [`.env.example`](../.env.example) to `.env` at repo root): **username** `admin@example.com`, **password** `Admin_ChangeMe_2026!`. If you changed `APP_SUPER_ADMIN_EMAIL` / `APP_SUPER_ADMIN_PASSWORD`, use those values instead ([root README](../README.md#ui-login-default-values)).

## Build

```bash
npm run build
```

This runs `ng build` (default **production** configuration) then `scripts/patch-index-html.mjs` so hashed `styles-*.css` / `main-*.js` / `chunk-*.js` links in `index.html` are **root-absolute** (`/…`), which avoids broken asset URLs on deep links when a client ignores `<base href="/">`. The same step normalizes **`favicon.svg`** (and legacy `favicon.ico` if present) to `/favicon.svg…` so the icon resolves from the site root.

Output (Angular application builder): **`dist/frontend/browser/`** — that folder is what you copy to a static host ([manual deployment](https://angular.dev/tools/cli/deployment)).

## Docker (local / Compose)

Aligned with Angular’s deployment guide: **multi-stage image** — Node stage runs `npm ci` + `npm run build`, then static files are copied into **nginx**.

- **Build stage:** `ENV NODE_ENV=development` so `npm ci` installs **devDependencies** (Tailwind, Postcss, DaisyUI, `@angular/build`, etc.); without this, production installs can skip them and ship broken CSS.
- **Runtime:** `nginx.conf` serves `browser/` output, uses **`try_files $uri $uri/ /index.html`** for SPA deep links, and a regex location so real **`.css` / `.js`** files never fall back to `index.html` (which would break MIME types).
- **Compose:** from the repo root, service **`web`** publishes **:8080**; API **`app`** is **:8081** for direct access; the UI still calls `/api` on **:8080** through the nginx proxy.
- **Security headers:** `nginx.conf` sets **CSP** (strict `script-src 'self'` — theme bootstrap lives in `public/theme-init.js`, not inline in `index.html`), **X-Content-Type-Options**, **Referrer-Policy**, **Permissions-Policy**. Adjust **`connect-src`** if the browser must talk to other origins.

```bash
# from repository root
docker compose build --no-cache web
docker compose up -d web
```

## Tests

```bash
npm test
```

### E2E smoke (Playwright)

```bash
# install browsers once
npx playwright install

# run the critical-path smoke
npm run test:e2e:smoke
```

Defaults:
- `E2E_BASE_URL=http://localhost:8080`
- `E2E_USER=admin@example.com`
- `E2E_PASSWORD=Admin_ChangeMe_2026!`

The smoke verifies: login -> branches list -> create branch -> back to list with created code visible.

## i18n (EN/ES)

- Runtime i18n is implemented with a lightweight **`I18nService`** (`core/i18n`), **not** `@ngx-translate/core` and **not** Angular compile-time `$localize` / XLF extraction.
- Locale is persisted via `UserPreferencesService` (`spring-web.pref.v1.locale`), and can be switched from login and shell headers.
- Supported locales: `en`, `es`.

**Why no ngx-translate?** For this portfolio scope, a small dictionary keeps dependencies low and the contract easy to explain in interviews. See [docs/security.md §2](../docs/security.md#2-internationalization--do-we-use-angular-translate--ngx-translate) for trade-offs.

Extra dictionary keys used by the shell: e.g. **`browserTabBrand`** (suffix for `document.title`), **`footerAuthor`** (footer credit), **`branchInactive`** (detail badge).

## Branding & shell UX

| Topic | Implementation |
|-------|----------------|
| **Favicon** | `public/favicon.svg` — linked from `src/index.html` (SVG only; avoids browsers picking a stale default `.ico`). |
| **Tab title** | `app.ts`: on each `NavigationEnd`, reads the leaf route’s `data['titleKey']` and sets `title` to `{translated title} · {browserTabBrand}`; reacts to locale changes. |
| **Account menu** | `app-settings-dropdown`: in the shell, `variant="account"` shows **user icon + camelCase username** (`displayUsername` pipe) and bundles **language**, **theme**, and **logout** in one dropdown. Login keeps the default **`variant="gear"`** (icon only). |
| **Footer** | `shared/footer` — portfolio line + **`footerAuthor`** + contact links. |

## Reusable loading (shared UI)

| Selector | Location | Purpose |
|----------|----------|---------|
| **`app-loading-spinner`** | `shared/ui/loading-spinner` | Small atomic spinner (`xs`–`lg`) for buttons, overlays, or inline use. |
| **`app-loading-state`** | `shared/ui/loading-state` | Block loading UI; optional **`skeleton`**: `none` (spinner only), `table`, `card`, `form` (pulse placeholders + caption). |
| **`app-busy-section`** | `shared/ui/busy-section` | Wraps forms/cards; when **`busy`** is true, frosted overlay + spinner + optional **label** (e.g. saving). |

**Usage in this app:** branch **list** / **detail** / **edit** (initial load) use skeleton presets where it helps perceived performance; **login**, **create**, and **edit** (submit) wrap the form in **`app-busy-section`** and show **`app-loading-spinner`** inside the primary button while the request runs.

## Production build (minification)

`npm run build` uses the **production** configuration by default: scripts and styles are **minified**, outputs are **hashed**, and `environment.prod.ts` is applied. This is **not** “encrypting” or hiding code; see [docs/security.md §3–4](../docs/security.md#3-production-frontend-builds--minification-and-bundles).

## OpenAPI generation (typed contract)

Generate/update the frontend OpenAPI typings from the backend:

```bash
# default source: http://localhost:8081/v3/api-docs
npm run openapi:generate
```

Optional custom spec URL:

```bash
OPENAPI_SPEC_URL=http://localhost:8081/v3/api-docs npm run openapi:generate
```

| Artifact | Role |
|----------|------|
| `src/app/core/models/openapi.generated.ts` | Generated **`paths`**, **`components.schemas`**, operations (do not edit by hand). |
| `src/app/core/models/api-types.ts` | App-facing aliases (`Branch`, `LoginRequest`, …) with `NonNullable` where the UI treats fields as required. |
| `src/app/core/api/api-paths.ts` | **`ApiPaths`**: static route strings passed through `apiPath<keyof paths>()` so a typo breaks the TypeScript build if the spec renames a path. |

Services use `apiUrl(API_BASE_URL, ApiPaths.…)` plus the types above so the HTTP contract stays aligned with Spring.

## Milestones **F1** & **F2** (see [docs/roadmap/status.md](../docs/roadmap/status.md))

- **F1:** login + paged branch list + create branch; theme + locale; OpenAPI typings + `ApiPaths`; unit tests + smoke E2E.
- **F2:** branch **detail** (`/branches/:id`), **edit** (`/branches/:id/edit`), **deactivate** (confirm + PATCH); smoke E2E covers the full flow.
- **IAM1 profile (v1.1):** **`/me`** — current user from `GET /api/v1/me` (shell nav **Profile** / **Perfil**).
- **IAM3 security (v1.4):** **`/me/security`** — list/revoke refresh sessions; **TOTP enrollment** for `SUPER_ADMIN` / `APP_ADMIN`; login **MFA** step when `mfaChallengeToken` is returned. IAM3 paths use `iam3Path()` in `api-paths.ts` until `openapi:generate` includes them.
- **H1:** **`refreshInterceptor`** (401 → cookie refresh → one retry); Nginx **CSP** + headers; **`public/theme-init.js`** for FOUC under CSP.
- Quality gate: `npm test`, `npm run test:e2e:smoke`, and `npm run openapi:generate` (with backend up) when the contract changes.

## Folder layout

```text
src/app/
  core/           # Auth, interceptors, preferences, i18n, `api/api-paths`, `models`, branch API client
  features/       # auth/login; branches list, create, detail, edit (.ts + .html)
  layout/         # App shell (nav, account menu, router-outlet) — .ts + .html
  shared/
    footer/       # Site footer; reused in shell + login
    pipes/        # e.g. `displayUsername` (camelCase label)
    ui/           # inline-alert, icons, page-nav, settings/account dropdown, loading-*
```

## Documentation

- [Repository hub](../README.md)
- [Roadmap & Postman](../docs/README.md)

## Contract notes

- List: `GET /api/v1/branches?page=&size=&sort=` → `PagedResponse` (`content`, `totalElements`, `page`, `size`, `totalPages`). `sort=property,asc|desc` (allowed fields: backend `BranchListPagination`).
- **List UI:** default `sort=code,asc`; `list()` cancels the in-flight request (`switchMap` + `takeUntilDestroyed`); hide `app-page-nav` when `totalElements === 0`; sortable headers: `app-sortable-th` + `shared/util/table-sort.ts`.
- Create: `POST /api/v1/branches` (`CreateBranchRequest`).
- Detail / update / deactivate: `GET|PUT /api/v1/branches/{id}`, `PATCH .../deactivate`.
- Login / logout / refresh: `POST /api/v1/auth/login|logout|refresh` — JWT en memoria; refresh vía cookie HttpOnly.
