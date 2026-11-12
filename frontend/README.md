# Branch Hub — Web (Angular)

Angular SPA for Branch Hub (npm package `branch-hub-web`): **login**, a post-login **dashboard**, the **branch registry** (paged list, detail, add, edit, deactivate), **admin** screens for users, roles and the audit log (SUPER_ADMIN), and **my profile**. It targets the backend contract under `/api/v1` and surfaces **Problem Details** messages when the API returns them. The visual identity is described in [Design direction](#design-direction).

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
| `@plugin "daisyui" { themes: false; include: … }` | Emits only the components the app uses. `themes: false` stops DaisyUI from emitting its stock themes, because the two `@plugin "daisyui/theme"` blocks below override **light** (default) and **dark** (`prefersdark`) with the brand tokens. The names stay `light`/`dark`, so `data-theme` on `<html>` (`public/theme-init.js`, settings menu) works unchanged. |
| `@import "@fontsource-variable/overpass"` | Self-hosted Overpass Variable, served from `'self'` (no CSP change). `@theme { --font-sans }` makes it the base font. |
| `angular.json` → `styles` | Includes `src/styles.css` so the CLI runs PostCSS on the global stylesheet. |

Production build turns off `inlineCritical` for styles so the main stylesheet is not deferred via the `media="print"` trick (avoids unstyled first paint in some browsers).

## Design direction

SPEC 03 ([specs/03-frontend-visual-redesign.md](../specs/03-frontend-visual-redesign.md)) gave the SPA its own identity. The concept is **wayfinding**: branches are stops on a network, so the visual language borrows from route maps and road signs. That means clear type, one strong route color, an amber signal for exceptions, and quiet surfaces. The one bold element is the route-map drawing on the login page. It draws itself in once, and shows still under `prefers-reduced-motion: reduce`.

### Color tokens

The hex values are the source of truth. `src/styles.css` holds their OKLCH conversions and sets **every** DaisyUI color token in both themes, so a DaisyUI upgrade cannot change a default the app relies on. Templates use the theme-independent `bg-paper`, `bg-surface` and `border-rule` utilities.

| Token | Light | Dark | Role |
| ----- | ----- | ---- | ---- |
| Paper | `#F6F7F5` | `#121920` (Night) | Page background |
| Surface | `#FFFFFF` | `#1A232C` | Panels, tables, sidebar |
| Ink | `#1C2631` | `#E4E9E6` | Body text, headings |
| Route green | `#0E7A5E` | `#3FBF94` | `primary`: primary actions, active nav, route lines, links |
| Signal amber | `#E3A21A` | `#E8B64A` | `warning`: inactive branches. Used as a fill or marker, never as text on Paper |
| Rule grey | `#D3D9D5` | `#2C3740` | Borders, dividers, table rules |

Contrast (WCAG 2.x ratios, computed from the hex values):

| Theme | Pair | Ratio |
| ----- | ---- | ----- |
| Light | Ink on Paper / on Surface | 14.25:1 / 15.32:1 |
| Light | Route green on Surface / on Paper (links) | 5.30:1 / 4.93:1 |
| Light | White on Route green (primary button) | 5.30:1 |
| Light | Ink on Signal amber | 6.89:1 |
| Dark | Ink on Night / on Surface | 14.42:1 / 12.95:1 |
| Dark | Route green on Night / on Surface (links) | 7.67:1 / 6.88:1 |
| Dark | Night on Route green (primary button) | 7.67:1 |
| Dark | Night on Signal amber | 9.46:1 |

The light-theme amber marker on Paper is 2.07:1. That is acceptable only because the marker never carries status alone: it always sits next to the word "Inactive".

### Type and shape

- **Overpass Variable** for everything. It is the open-source descendant of Highway Gothic, the US road-sign typeface. Its Latin subset has tabular numerals, used through `tabular-nums` for branch codes, counts, dates and table numbers.
- Scale of about 1.25: 14 / 16 / 20 / 25 / 31 px. Body is 16 px, tables and secondary text 14 px. Weights: 400 body, 600 headings and active nav, 700 only for the dashboard numbers.
- Radii follow hierarchy: inputs and buttons small (`--radius-field`), panels and modals medium (`--radius-box`), badges pill (`--radius-selector`). `--depth: 0`, so panels have no shadows.

### Design rules

Every template follows these. Check new screens against them.

1. **Structure carries meaning.** Route-line markers appear only on real sequences: the dashboard activity timeline and the login drawing. Borders separate regions.
2. **No decorative chrome.** No gradients, all caps, tracked-out or eyebrow labels, `→` on links, monospace data labels, or `A · B · C` meta strings.
3. **Sentence case** for every heading, button, label and menu item, in `en` and `es`.
4. **Actions keep their verb in the toast.** "Add branch" → "Branch added", "Save changes" → "Changes saved", "Deactivate branch" → "Branch deactivated", "Create user" → "User created", "Save roles" → "Roles saved", "Disable user" → "User disabled".
5. **Empty and error states give direction.** Empty states name the next action ("No branches yet. Add the first one."). Errors say what failed and what to do ("Couldn't load branches. Check your connection and retry."), without apologizing.
6. **Motion** happens only in response to the user (drawer, sidebar collapse, modal), except the login drawing, and respects reduced motion.
7. **Status uses color plus text** (`app-status-marker`): an amber marker plus "Inactive".

### Shell and layout

| Topic | Behavior |
| ----- | -------- |
| **Sidebar** (`lg` and up) | Dashboard, Branches, Admin (SUPER_ADMIN only) and Profile, each with an icon and a route-green active bar. It collapses to an icon rail; rail items keep an `aria-label` and a tooltip. The state is saved under `branch-hub.pref.v1.sidebar` (`expanded` default, or `collapsed`; unknown values fall back to `expanded`). |
| **Drawer** (below `lg`) | The same nav in a DaisyUI drawer, opened from the topbar hamburger (`drawer-toggle`). Choosing an item navigates and closes it. |
| **Topbar** | Page title from the route's `data['titleKey']`, the account/settings menu and the logout action. |
| **Login** | Split screen from `md` up: the brand panel (route-map drawing, logo, tagline) next to the form card. Below `md`, only the logo and the form. The settings menu stays reachable at every width. |
| **Dashboard** (`/dashboard`, post-login home; `/` redirects here) | A summary sentence built from live counts ("{n} branches in the registry.", plus "{n} user accounts." for SUPER_ADMIN), the quick actions "Add branch" and "Create user" (SUPER_ADMIN, opens the create-user modal), and, for SUPER_ADMIN, the latest 5 audit events as a timeline. It reuses the list query keys, so mutations keep it fresh. Admin queries are `enabled` only for SUPER_ADMIN, so an APP_ADMIN never calls `/api/v1/admin/**`. |
| **Tables on phones** | Below `sm`, the branches list, admin users and audit log render one card per row, with a sort select in place of the column headers. The full table shows from `sm` up. |
| **Sort headers** | Only the active column shows its arrow. Idle columns show the sort hint on hover or keyboard focus. |
| **Logo** | `shared/ui/logo` draws the branching-node mark in `currentColor`. The same mark is `public/favicon.svg`. |

### Screenshots for design review

`e2e/screenshots.spec.ts` captures every screen at 390 px and 1280 px, in light and dark, into `e2e/__screenshots__/` (gitignored). It makes no pixel-diff assertions, because font rendering differs across hosts; review the images against the rules above. It needs the Compose stack and a SUPER_ADMIN login (the same `E2E_*` variables as the smoke tests), and adds one branch only when the registry is empty.

```bash
npm run test:e2e:screenshots
DOCS_IMAGES=1 npm run test:e2e:screenshots   # also refreshes the root README images (login, dashboard, branches, admin users) in ../docs/images/
```

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
- For local dev: `npm start` proxies `/api` to the **Docker Compose** API at `http://localhost:18091` (`proxy.conf.json`). The Compose SPA is at **http://127.0.0.1:18090** (`web`) and calls relative `/api`, which Nginx forwards to Spring. To use a `./gradlew bootRun` API on port 8080 instead, change the proxy `target` locally (do not commit it).

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

Open `http://localhost:4200/`. Default credentials (if you copied [`.env.example`](../.env.example) to `.env` at repo root): **username** `admin@example.com`, **password** `Admin_ChangeMe_2026!`. If you changed `APP_SUPER_ADMIN_EMAIL` / `APP_SUPER_ADMIN_PASSWORD`, use those values instead ([getting started](../docs/getting-started.md#default-login)).

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
- **Compose:** from the repo root, service **`web`** publishes **127.0.0.1:18090**; API **`app`** is **127.0.0.1:18091** for direct access; the UI still calls `/api` on its own origin through the nginx proxy.
- **Proxy list:** nginx proxies `/api/`, `^~ /swagger-ui/` (prefix match so Swagger's `.js`/`.css` skip the static-asset regex), `= /swagger-ui.html`, `/v3/` and `= /actuator/health` to `app:8080`. Every other `/actuator/*` path returns 404. `X-Forwarded-Proto` keeps the incoming header when one is present (`cloudflared` sends `https`) and falls back to `$scheme`.
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

# run the critical-path smoke and the admin smoke
npm run test:e2e:smoke
npm run test:e2e:admin-smoke
```

Defaults:
- `E2E_BASE_URL=http://localhost:18090`
- `E2E_USER=admin@example.com`
- `E2E_PASSWORD=Admin_ChangeMe_2026!`

The smoke verifies: login → dashboard → branches list (through `nav-branches`) → add branch → detail → edit → deactivate. The admin smoke signs in, opens the admin area through `nav-admin` and creates a user. Both run at the Desktop Chrome viewport, so the full tables render. The screenshot capture is described in [Screenshots for design review](#screenshots-for-design-review).

Playwright does not support every Linux distribution (for example Ubuntu 26.04). There, run the suites in the official image that matches the installed version, against the Compose stack:

```bash
docker run --rm --network host --ipc=host --user "$(id -u):$(id -g)" -e HOME=/tmp \
  -v "$PWD":/work -w /work -e E2E_BASE_URL=http://localhost:18090 -e E2E_PASSWORD \
  mcr.microsoft.com/playwright:v1.59.1-noble \
  sh -c "npm run test:e2e:smoke && npm run test:e2e:admin-smoke && npm run test:e2e:screenshots"
```

## i18n (EN/ES)

- Runtime i18n is implemented with a lightweight **`I18nService`** (`core/i18n`), **not** `@ngx-translate/core` and **not** Angular compile-time `$localize` / XLF extraction.
- The dictionaries live in `core/i18n/`. `translations.ts` holds `en`, which defines the key set and ships in the initial bundle. `translations.es.ts` holds `es`, typed against it, so a missing or extra Spanish key fails the build. `es` is lazy-loaded: `setLocale` switches only once it has arrived, and the app initializer waits for it when Spanish is stored, so the first paint is never in the wrong language. `translations.spec.ts` also checks that both have the same keys and the same `{placeholders}`.
- There is no plural support. Sentences with counts use separate `…One` / `…Other` keys chosen in the component (for example `dashboardBranchesOne` / `dashboardBranchesOther`), never string concatenation, so Spanish grammar stays correct.
- Audit `action` codes map to readable labels in `features/admin/audit-action-label.ts`, shared by the audit log and the dashboard timeline. Unknown codes fall back to the raw code.
- Locale is persisted via `UserPreferencesService` (`branch-hub.pref.v1.locale`), and can be switched from the settings menu on the login page and in the shell.
- Supported locales: `en`, `es`. Copy follows the [design rules](#design-rules).

**Why no ngx-translate?** For this portfolio scope, a small dictionary keeps dependencies low and the contract easy to explain in interviews. See [docs/security.md §2](../docs/security.md#2-internationalization--do-we-use-angular-translate--ngx-translate) for trade-offs.

Extra dictionary keys used by the shell: e.g. **`browserTabBrand`** (suffix for `document.title`), **`footerAuthor`** (footer credit), **`branchInactive`** (detail badge).

## Branding & shell UX

| Topic | Implementation |
|-------|----------------|
| **Favicon** | `public/favicon.svg` — the logo mark, linked from `src/index.html` (SVG only; avoids browsers picking a stale default `.ico`). |
| **Tab title** | `index.html` starts with **Branch Hub**. `app.ts`: on each `NavigationEnd`, reads the leaf route’s `data['titleKey']` and sets `title` to `{translated title} · {browserTabBrand}` ("Branch Hub"); reacts to locale changes. |
| **Account menu** | `app-settings-dropdown`: in the topbar, `variant="account"` shows **user icon + camelCase username** (`displayUsername` pipe) and bundles **language**, **theme**, and **logout**. Login keeps the default **`variant="gear"`** (icon only). |
| **Feedback** | `UiFeedbackService` toasts confirm every successful mutation with the action's verb (see design rule 4). Errors stay inline (`app-inline-alert`) with a title that names what failed. |
| **Footer** | `shared/footer` — project name (**`footerPortfolio`**) + **`footerAuthor`** + links to jotaeme.dev, GitHub and LinkedIn. |

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
# default source: http://localhost:18091/v3/api-docs
npm run openapi:generate
```

Optional custom spec URL:

```bash
OPENAPI_SPEC_URL=http://localhost:18091/v3/api-docs npm run openapi:generate
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
- **H1:** **`refreshInterceptor`** (401 → cookie refresh → one retry); Nginx **CSP** + headers; **`public/theme-init.js`** for FOUC under CSP.
- Quality gate: `npm test`, `npm run test:e2e:smoke`, and `npm run openapi:generate` (with backend up) when the contract changes.

## Folder layout

```text
src/app/
  core/           # Auth, interceptors, preferences, i18n, `api/api-paths`, `models`, branch API client
  features/       # auth (login + route-map art), dashboard, branches, admin, account (.ts + .html)
  layout/         # App shell: sidebar, drawer, topbar, router-outlet — .ts + .html
  shared/
    footer/       # Site footer; reused in shell + login
    pipes/        # e.g. `displayUsername` (camelCase label)
    ui/           # logo, icons, inline-alert, status-marker, sort-select, sortable-th, page-nav, settings dropdown, loading-*
    util/         # table-sort, media-query, relative-time
```

## Documentation

- [Project README (root)](../README.md)
- [Getting started](../docs/getting-started.md)
- [Architecture](../docs/architecture.md)
- [Roadmap & Postman](../docs/README.md)

## Contract notes

- List: `GET /api/v1/branches?page=&size=&sort=` → `PagedResponse` (`content`, `totalElements`, `page`, `size`, `totalPages`). `sort=property,asc|desc` (allowed fields: backend `BranchListPagination`).
- **List UI:** default `sort=code,asc`; `list()` cancels the in-flight request (`switchMap` + `takeUntilDestroyed`); hide `app-page-nav` when `totalElements === 0`; sortable headers: `app-sortable-th` + `shared/util/table-sort.ts`.
- Create: `POST /api/v1/branches` (`CreateBranchRequest`).
- Detail / update / deactivate: `GET|PUT /api/v1/branches/{id}`, `PATCH .../deactivate`.
- Login / logout / refresh: `POST /api/v1/auth/login|logout|refresh` — JWT kept in memory; refresh through the HttpOnly cookie.
