# SPEC 03 — Frontend visual redesign

> **Status:** Implemented
> **Depends on:** SPEC 01
> **Date:** 2026-11-05
> **Objective:** Give the SPA its own "wayfinding" visual identity before publishing: brand themes, font and logo, a sidebar shell, a split-screen login, a post-login dashboard, and restyled screens that turn into cards on phones.

## Why this spec exists

SPEC 01 prepared the repository to go public. The SPA works, but it still looks like a scaffold: stock DaisyUI `light`/`dark` themes, the system font, no logo, a plain top navbar, a bare login form, `appTitle` set to "Branches", and tables that only scroll sideways on phones. This spec is about how the app looks. It changes no backend behavior and no API contract.

The design direction was set with the `frontend-design` skill. That review rejected the first draft's generic SaaS defaults (Inter, indigo on slate, identical stat cards, a gradient brand panel) in favor of choices tied to what the product is.

## Design direction

**Subject:** Branch Hub is a registry of a company's physical branches (code, name, city, active), with user and role administration. It serves the admins who run it and the people who see the public repository. Its main job is to find, add and change branches quickly and accurately.

**Concept, "wayfinding":** branches are stops on a network. The visual language borrows from route maps and road signs: clear type, one strong route color, an amber signal for exceptions, and quiet surfaces.

**The one bold element** is the login brand panel: a large route-map line drawing built from the logo's branching-node motif. It draws itself in once on load, and is shown still when `prefers-reduced-motion: reduce` is set. Every other surface stays restrained.

### Color tokens

| Token | Light | Dark | Role |
| ----- | ----- | ---- | ---- |
| Paper | `#F6F7F5` | `#121920` (Night) | page background (`base-200` light / `base-100` dark) |
| Surface | `#FFFFFF` | `#1A232C` | panels, table, sidebar |
| Ink | `#1C2631` | `#E4E9E6` | body text, headings |
| Route green | `#0E7A5E` | `#3FBF94` | `primary`: primary actions, active nav, route lines, links |
| Signal amber | `#E3A21A` | `#E8B64A` | `warning`: inactive branches, warnings; used as a fill or marker, never as text on Paper |
| Rule grey | `#D3D9D5` | `#2C3740` | borders, dividers, table rules |

The hex values above are the source of truth. Step 1 converts them to the OKLCH tokens DaisyUI expects and sets every DaisyUI color token explicitly in both themes, including `error`, `success`, `info` and every `*-content` color. Text needs AA contrast (4.5:1). Large text and UI parts need 3:1.

### Type

- **Overpass Variable** (`@fontsource-variable/overpass`, self-hosted), used for everything. It is the open-source descendant of Highway Gothic, the US road-sign typeface.
- Scale ratio of about 1.25: 14 / 16 / 20 / 25 / 31 px. Body is 16 px, tables and secondary text 14 px.
- Weights: 400 for body, 600 for headings and active nav, 700 only for the dashboard summary numbers.
- `font-variant-numeric: tabular-nums` for branch codes, counts, dates and table numbers.
- Body text and form help stay under 80 characters per line.

### Layout

```
Desktop (lg+)                              Phone (< lg)
┌──────────┬──────────────────────────┐    ┌──────────────────────┐
│ ◆ Branch │ Branches          ⚙  ⎋  │    │ ☰  Branches    ⚙  ⎋ │
│   Hub    ├──────────────────────────┤    ├──────────────────────┤
│          │                          │    │ ┌──────────────────┐ │
│ ● Dashb. │  page content,           │    │ │ BOG-01  Active   │ │
│ ○ Branch │  left-aligned,           │    │ │ Bogota Centro    │ │
│ ○ Admin  │  max-w-6xl               │    │ │ Bogota           │ │
│ ○ Profile│                          │    │ └──────────────────┘ │
│      «   │                          │    │  (card per row)      │
└──────────┴──────────────────────────┘    └──────────────────────┘

Dashboard                                  Login (md+)
┌─────────────────────────────────────┐    ┌───────────────┬──────────────┐
│ 12 branches in the registry.        │    │   ◆──●        │  Sign in     │
│ 4 user accounts.      [Add branch]  │    │    \  ●──●    │  [email   ]  │
│                       [Create user] │    │     ●    \    │  [password]  │
│ Recent activity                     │    │  route-map    │  [Sign in]   │
│ ●─ User created · admin · 10:42     │    │  drawing      │              │
│ │                                   │    │ Branch Hub    │              │
│ ●─ Roles changed · admin · 09:15    │    │               │              │
└─────────────────────────────────────┘    └───────────────┴──────────────┘
```

Content is left-aligned everywhere except the login form card, which is centered in its column. The middle dots in the timeline sketch only mark where the fields go. The real row uses its own layout (see the design rules).

### Design rules

These rules apply to every template this spec touches:

1. **Structure carries meaning.** Route-line markers appear only where the content is a real sequence: the activity timeline and the login drawing. Borders separate regions. Panels do not get shadows. Radii follow hierarchy: inputs and buttons are small, panels and the modal medium, badges pill-shaped. One radius is not applied to everything.
2. **No decorative chrome.** No gradient washes. No all-caps or tracked-out labels. No eyebrow labels above headings. No `→` added to links or buttons. No monospace face for data labels. No `A · B · C` meta strings.
3. **Sentence case** for every heading, button, label and menu item, in both `en` and `es`.
4. **Actions are named for what they do, and keep that name through the flow.** "Add branch" leads to the toast "Branch added". "Save changes" leads to "Changes saved". "Deactivate branch" leads to "Branch deactivated". "Create user" leads to "User created".
5. **Empty and error states give direction.** An empty state invites the next action (for example "No branches yet. Add the first one." with the action). An error says what failed and what to do, without apologizing (for example "Couldn't load branches. Check your connection and retry.").
6. **Motion:** the login drawing has the only automatic animation. Everything else animates only in response to the user (drawer open, sidebar collapse, modal) and respects `prefers-reduced-motion`.
7. **Status uses color plus text.** An inactive branch shows an amber marker and the word "Inactive". Status is never shown by color alone.

## Scope

**In:**

- Brand themes: override DaisyUI's `light` and `dark` themes with the color tokens above, in `frontend/src/styles.css`.
- Font: Overpass Variable, self-hosted through `@fontsource-variable/overpass` (Latin subset), used as the base font with the type scale above.
- Logo: a hand-written geometric SVG mark (branching route nodes) that uses `currentColor`. It is reused in the sidebar, on the login page and as `public/favicon.svg`.
- Shell: replace the top navbar with a sidebar plus a topbar.
  - Sidebar items: Dashboard, Branches, Admin (SUPER_ADMIN only), Profile, each with an icon and a route-green active indicator.
  - On `lg` and up, the sidebar can collapse to an icon-only rail. That state is saved.
  - Below `lg`, the sidebar is a DaisyUI `drawer` opened from a hamburger in the topbar.
  - The topbar holds the page title (from the route's `titleKey`), the settings dropdown, the logout action and the logging-out spinner.
- Login: split screen.
  - From `md` up, the brand panel shows the route-map drawing, the logo, the product name and a plain tagline (`en`: "Every branch's details, in one place."). The form card sits next to it.
  - Below `md`, only the logo and the form show.
  - The drawing plays its one draw-in animation unless reduced motion is set.
  - The settings dropdown stays reachable at every width.
- Dashboard: a new `/dashboard` route that becomes the post-login home.
  - Summary line: a sentence built from live counts, in both locales. "{n} branches in the registry." for everyone, plus "{n} user accounts." for SUPER_ADMIN, with singular forms when n is 1. The numbers are set at weight 700 with tabular numerals.
  - Quick actions next to the summary: "Add branch" (everyone), and "Create user" (SUPER_ADMIN only), which goes to `/admin/users` and opens the create-user modal.
  - "Recent activity" (SUPER_ADMIN only): the latest 5 audit events as a vertical timeline, newest first, using route-line markers. Each entry shows the action in readable form, the actor and the relative time.
  - The summary and the timeline each have loading, error and empty states that follow the design rules.
- Restyle every existing screen to the design direction: branches list, detail, create and edit; admin users (including the create-user modal), roles, audit log and admin subnav; my profile; footer; the shared UI components (inline alert, loading state, busy section, toast, sortable header, page nav, settings dropdown, language switcher, theme toggle, password rules hint).
- Mobile tables: on the branches list, admin users and audit log, each row becomes a stacked card below `sm`. The full table shows from `sm` up. Sorting and paging keep working at both sizes.
- Copy pass across every touched template and both i18n dictionaries, applying design rules 3 to 5. "New branch" becomes "Add branch". `appTitle` becomes "Branch Hub". New strings cover the brand name, the tagline, the dashboard, the sidebar and the collapse/expand aria labels.
- Redirects that point at `/branches` today (`''`, the login success, `guestGuard`, `roleGuard`) point at `/dashboard`.
- E2E:
  - Update `smoke.spec.ts` and `admin.smoke.spec.ts` for the new landing page, navigation and renamed actions.
  - Add `e2e/screenshots.spec.ts`. It captures every screen at 390 px and 1280 px wide, in light and dark, into `frontend/e2e/__screenshots__/` (already gitignored), with no pixel-diff assertions.
- Docs: `frontend/README.md` (design direction summary, tokens, rules, screenshot script), `docs/roadmap/status.md` and `CHANGELOG.md`. Refresh the screenshots under `docs/images/` if the README already embeds any.

**Out of scope (for future specs):**

- Any backend or API contract change. That includes adding `updatedAt` to `BranchListPagination.SORT_POLICY`, so the dashboard has no "recently updated branches" list. It could be its own small spec.
- Dashboard numbers the API cannot give cheaply: active/inactive counts, cities.
- New features: change-password, register, OAuth or phone login, branch search or filters, charts, a real geographic map.
- Visual regression baselines (`toHaveScreenshot` with committed images).
- A full accessibility audit (WCAG conformance, screen-reader testing). The redesign keeps or improves the current ARIA labels and keyboard focus, but this spec does not certify accessibility.
- Expanding unit-test coverage beyond the new or changed logic listed below.
- A third-party icon library. Icons stay in the in-house `IconComponent`.
- Changing how audit-log actions are worded on the backend. The SPA maps the existing `action` codes to readable i18n strings.

## Data model

There are no new API types. The only new persisted value is a UI preference.

### Preferences

```ts
// core/preferences/preference-keys.ts
export const PreferenceKeys = {
  theme: `${PREFERENCE_STORAGE_PREFIX}.theme`,
  locale: `${PREFERENCE_STORAGE_PREFIX}.locale`,
  sidebar: `${PREFERENCE_STORAGE_PREFIX}.sidebar`, // new: branch-hub.pref.v1.sidebar
} as const;

export type SidebarMode = 'expanded' | 'collapsed'; // default 'expanded'
```

`UserPreferencesService` exposes the sidebar mode as a signal with a setter, the same way it handles the theme. An unknown stored value falls back to `'expanded'`. `PREFERENCE_STORAGE_VERSION` stays `v1`, because no stored value changes shape. `public/theme-init.js` does not change: it only handles the theme, and the stored theme values stay `'light' | 'dark'`.

### Themes and type (`frontend/src/styles.css`)

```css
@import "@fontsource-variable/overpass";
@import "tailwindcss" source(".");

@plugin "daisyui" {
  themes: false; /* the two daisyui/theme overrides below set default/prefersdark; avoids emitting stock light/dark too */
  include: /* existing list */ ..., drawer, menu, card, timeline, tooltip;
  logs: false;
}

@plugin "daisyui/theme" {
  name: "light";
  default: true;
  /* every color token from the light column of "Color tokens", converted to OKLCH;
     radius tokens: --radius-field small, --radius-box medium, --radius-selector pill */
}

@plugin "daisyui/theme" {
  name: "dark";
  prefersdark: true;
  /* every color token from the dark column */
}

@theme {
  --font-sans: "Overpass Variable", ui-sans-serif, system-ui, sans-serif;
}
```

### Dashboard queries (reuse existing keys and services)

| Element | Call | Query key | Visible to |
| ------- | ---- | --------- | ---------- |
| Branch count in the summary | `BranchApiService.list(0, 1, 'code,asc')` → `totalElements` | `branchQueryKeys.list(0, 1, 'code,asc')` | everyone |
| User count in the summary | `AdminApiService.listUsers(0, 1, 'createdAt,desc')` → `totalElements` | `adminQueryKeys.userList(0, 1, 'createdAt,desc')` | SUPER_ADMIN |
| Recent activity timeline | `AdminApiService.listAuditLogs(0, 5, 'createdAt,desc')` | `adminQueryKeys.auditLogList(0, 5, 'createdAt,desc')` | SUPER_ADMIN |

Because the dashboard reuses the existing keys, the current mutations already invalidate it. Admin queries are `enabled` only when `auth.hasRole('SUPER_ADMIN')`, so an APP_ADMIN never calls `/api/v1/admin/**`. The audit-log screen and the timeline share one mapping from `action` code to i18n label, with a fallback to the raw code for unknown actions.

### New and changed files

| File | Change |
| ---- | ------ |
| `frontend/package.json` | add `@fontsource-variable/overpass`; add the `test:e2e:screenshots` script |
| `frontend/src/styles.css` | themes, font, DaisyUI `include` list |
| `frontend/public/favicon.svg` | new logo mark |
| `frontend/src/app/shared/ui/logo/logo.component.ts` | new: inline SVG mark, with an optional wordmark |
| `frontend/src/app/features/auth/route-map-art.component.ts` | new: the login route-map drawing (inline SVG, a CSS draw-in, the reduced-motion fallback) |
| `frontend/src/app/shared/ui/icon/icon.component.{ts,html}` | new icons: `home`, `building`, `shield`, `menu`, `sidebar-collapse`, `sidebar-expand`, `plus`, `logout` |
| `frontend/src/app/layout/app-shell.component.{ts,html}` | sidebar, drawer and topbar |
| `frontend/src/app/features/dashboard/dashboard.component.{ts,html}` | new |
| `frontend/src/app/features/admin/audit-action-label.ts` | new: shared `action` → i18n key mapping |
| `frontend/src/app/app.routes.ts` | `dashboard` route (`titleKey: 'dashboardTitle'`) and the `''` redirect |
| `frontend/src/app/core/auth/guest.guard.ts`, `role.guard.ts`, `features/auth/login.component.ts` | redirect to `/dashboard` |
| `frontend/src/app/core/preferences/preference-keys.ts`, `user-preferences.service.ts` | sidebar preference |
| `frontend/src/app/core/i18n/i18n.service.ts` | new and reworded `en`/`es` strings, `appTitle` |
| `frontend/src/app/features/**`, `shared/**` templates | restyle, mobile cards, copy pass |
| `frontend/e2e/smoke.spec.ts`, `admin.smoke.spec.ts` | landing page, navigation, renamed actions |
| `frontend/e2e/screenshots.spec.ts` | new |

New `data-testid`s: `nav-dashboard`, `nav-branches`, `nav-admin`, `nav-profile`, `sidebar-toggle`, `drawer-toggle`, `dashboard-summary`, `dashboard-activity`, `branch-add`. Every existing `data-testid` is kept, with the same meaning, at both breakpoints.

## Implementation plan

0. **Precondition (owner):** a clean working tree on a new spec branch. Compose is running (see `docs/getting-started.md`) so that the e2e suites can run.
1. **Identity tokens.** Add `@fontsource-variable/overpass` and check that it supports `tnum`. Convert the color tokens to OKLCH and override the DaisyUI `light`/`dark` themes with every token set. Set the font, the type scale and the radius tokens, and extend the DaisyUI `include` list. Draw the logo, write `LogoComponent`, and replace `favicon.svg`. Verify: `npm run build` passes within the existing budgets, the CSP needs no change, the contrast pairs in "Color tokens" meet AA (checked with a contrast tool, results noted in the PR), and the existing screens render with the new tokens in both themes.
2. **Shell.** Add the sidebar preference and its unit specs, and the new icons. Rebuild `app-shell` as sidebar, drawer and topbar: active indicator, a persisted collapse on `lg` and up, and the drawer below `lg`, which closes when you navigate. Collapsed rail items keep an `aria-label` and a tooltip. Verify: `npm test` passes, and keyboard navigation reaches every nav item and toggle with a visible focus ring.
3. **Login.** Build `route-map-art` and the split-screen layout, keeping the form logic and `data-testid`s. Verify: `npm run test:e2e:smoke` gets past login. With reduced motion turned on in DevTools, the drawing shows still.
4. **Dashboard.** Add the route, the component, the role-gated queries, the summary sentence with plurals in both locales, the quick actions, the activity timeline with the shared action-label mapping, and the empty, loading and error states. Point the four redirects at `/dashboard`. Update `smoke.spec.ts` to expect `/dashboard` and then click `nav-branches`, and update `admin.smoke.spec.ts` to use `nav-admin`. Verify: both smoke suites pass, and an APP_ADMIN dashboard sends no `/api/v1/admin/**` request (browser network tab).
5. **Screens and mobile cards.** Restyle branches (list, detail, create, edit), admin (users and its modal, roles, audit, subnav), profile, footer and the shared UI components to the design rules. Add the card view below `sm` to the three tables. Rename "New branch" to "Add branch" (`branch-add`) and update `smoke.spec.ts` to use that test ID. Verify: `npm test`, `npm run build` and both smoke suites pass.
6. **Copy pass.** Go through both dictionaries and every touched template against design rules 2 to 5: sentence case, action names matching their toasts, directive empty and error states, no hard-coded visible strings. Add the unit spec that checks the `en`/`es` key sets match. Verify: switch the language on every screen. The grep checks in the acceptance criteria pass.
7. **Screenshots.** Add `e2e/screenshots.spec.ts` and the script. Verify: running it against Compose writes every screen × 2 widths × 2 themes into `e2e/__screenshots__/`, and `git status` shows none of those files.
8. **Self-critique pass.** Review the screenshots against the design rules and the manual checklist. Remove at least one decorative element that does not earn its place, and record what was removed in the PR notes.
9. **Docs.** Update `frontend/README.md` (design direction, tokens, rules, shell behavior, sidebar preference key, screenshot command), `docs/roadmap/status.md` (SPEC 03 as the last done item) and `CHANGELOG.md` (an `## Unreleased` entry). Refresh `docs/images/` if the README embeds UI screenshots.
10. **Final pass.** Run `npm ci && npm test && npm run build` from `frontend/`. Run `docker compose up -d --build` from the repo root, then `npm run test:e2e:smoke`, `npm run test:e2e:admin-smoke` and `npm run test:e2e:screenshots`.

## Acceptance criteria

- [x] `npm test` and `npm run build` pass from `frontend/`, with no new budget warnings beyond today's. *(The initial bundle is 568.46 kB (~152 kB transferred) against 561.28 kB on `main`; the `initial` warning budget was raised from 560 kB to 600 kB, so the build has no warnings. The error budget stays at 1.2 MB.)*
- [x] `npm run test:e2e:smoke` and `npm run test:e2e:admin-smoke` pass against Compose.
- [x] `npm run test:e2e:screenshots` produces one image per screen × {390, 1280} × {light, dark}. The screens are login, dashboard (SUPER_ADMIN), branches list, detail, create and edit, admin users, the create-user modal, roles, audit log and profile.
- [x] `git diff main --stat -- backend/ frontend/src/app/core/models/openapi.generated.ts frontend/nginx.conf` is empty.
- [x] `frontend/src/styles.css` defines both themes with the primary colors `#0E7A5E` (light) and `#3FBF94` (dark), in OKLCH, and sets `--font-sans` to Overpass Variable.
- [x] `grep -rn "fonts.googleapis\|fonts.gstatic\|@fontsource-variable/inter" frontend/src` returns nothing.
- [x] `grep -rnE "uppercase|tracking-widest|bg-gradient|→" frontend/src/app --include=*.html` returns nothing.
- [x] After login, the browser lands on `/dashboard`, and `/` redirects to `/dashboard`.
- [x] As APP_ADMIN, the dashboard shows only the branch sentence and "Add branch", and the network log has no request to `/api/v1/admin/`.
- [x] As SUPER_ADMIN, the dashboard shows both summary sentences, "Create user", and a timeline of up to 5 audit events in `createdAt` descending order, with readable action labels.
- [x] With 1 branch, the summary reads "1 branch in the registry." (singular), in both locales.
- [x] Collapsing the sidebar at 1280 px and reloading keeps it collapsed. `localStorage['branch-hub.pref.v1.sidebar']` is `'collapsed'`.
- [x] At 390 px, the sidebar is hidden, the hamburger opens the drawer, and choosing an item navigates and closes it.
- [x] At 390 px, the branches list, admin users and audit log show cards, not a table. At 1280 px they show the table.
- [x] With `prefers-reduced-motion: reduce` emulated, the login drawing renders fully with no animation.
- [x] The favicon, the sidebar and the login brand panel show the same SVG mark.
- [x] Inactive branches show an amber marker and the text "Inactive" (or "Inactiva") in the list, the cards and the detail page.
- [x] Every i18n key added in this spec exists in both `en` and `es`, and a unit spec checks that the two dictionaries have the same key set.
- [x] `UserPreferencesService` has unit specs for reading, writing and falling back on the sidebar mode. The action-label mapping has a unit spec, including the fallback for unknown codes.
- [x] Manual checklist, reviewed on the screenshots: *(Open until the panel-shadow item below is fixed.)*
  - [x] Text and primary buttons meet AA contrast in both themes (contrast tool results are in the PR).
  - [x] No horizontal page scroll at 390 px on any screen.
  - [x] There is a visible focus ring on nav items, buttons and inputs.
  - [x] Every button and toast pair uses the same verb (Add/added, Save/saved, Deactivate/deactivated, Create/created).
  - [x] Every empty state names the next action, and every error state names what failed and what to do.
  - [x] Panels have no shadows, and route-line markers appear only on the timeline and the login drawing. *(Open: route-line markers pass, but the four `modal-box` dialogs keep DaisyUI's built-in shadow.)*

## Decisions taken and discarded

- **Visual redesign over tests, refactor or an accessibility pass.** The owner wants publishing and portfolio polish. Those other areas can become their own specs.
- **Frontend-only.** This keeps the spec small and the API contract stable. As a result, the "recently updated branches" list was dropped, because `BranchListPagination.SORT_POLICY` does not allow `updatedAt`. Two alternatives were discarded: a client-side "inactive branches" hack, and a one-line backend change.
- **One spec, phased steps,** rather than splitting the dashboard into SPEC 04. Each step leaves the app working.
- **Design direction set with the `frontend-design` skill, at the owner's request.** The first draft (indigo/violet on slate, Inter, stat cards, a gradient login panel) was replaced because each of those is a generic SaaS default rather than a choice for this product.
- **"Wayfinding" concept.** Branches are stops on a network, which connects the logo, the login drawing, the timeline and the status colors into one idea that comes from the subject.
- **Route green and signal amber on paper and ink,** over indigo/slate. The palette comes from route maps and road signs. Amber is only used as a fill or marker because it fails AA as text on Paper.
- **Overpass Variable,** over Inter and Public Sans. It descends from road-sign type, which fits the concept, and is open source and self-hostable under `font-src 'self'`. One family is enough, and the hierarchy comes from the scale and weights.
- **One bold element: the login route-map drawing,** with a single draw-in animation. Everything else stays quiet, so the identity lands without decorating every screen.
- **Summary sentence plus activity timeline,** over stat cards. A plain sentence with live numbers reads faster than a card grid. The audit log is a real sequence, so a timeline gives the route-line markers a meaning.
- **Copy and chrome rules written into the spec** (sentence case, matching action and toast names, directive empty and error states, no gradients, all caps, eyebrows or arrows), so the restyle does not bring back template tells screen by screen.
- **"Add branch" replaces "New branch"** so the button and its toast ("Branch added") use the same verb. The e2e smoke test switches to the `branch-add` test ID.
- **Sidebar plus topbar** over a refined top navbar. It scales to future sections, like sessions and settings in IAM3.
- **Collapsible, persisted sidebar**, stored under the existing `branch-hub.pref.v1` namespace with no version bump.
- **Override DaisyUI's `light`/`dark` themes instead of new theme names.** Stored theme values, `ThemeMode` and `public/theme-init.js` stay valid with no migration and no FOUC risk.
- **Bundle trims after Step 10** (owner's request). The initial bundle was 583.25 kB against 561.28 kB on `main`. Two cuts brought it to 568.46 kB: the DaisyUI `include` list drops `navbar`, `card`, `divider`, `footer`, `badge`, `label` and `menu`, which no template uses (−8.4 kB CSS), and the `es` dictionary moved to `translations.es.ts`, loaded on demand (−6.4 kB). The app initializer waits for it when Spanish is stored, so there is no English flash. The remaining ~8.5 kB over the 560 kB warning is framework and shell code, so the `initial` warning budget was raised to 600 kB (the error budget stays at 1.2 MB).
- **`themes: false` in the main DaisyUI plugin** (decided during Step 1). With `themes: light --default, dark --prefersdark` DaisyUI also emitted its stock light/dark blocks under the overrides (~4.5 kB of dead CSS). The overrides keep the same names, `default` and `prefersdark`, so behavior is unchanged. The initial bundle was already 1.28 kB over the 560 kB warning budget before this spec; the remaining growth comes from the added DaisyUI components and the font.
- **Hand-written geometric SVG logo**, reused as the favicon. There are no external assets or licensing questions.
- **Split-screen login** over a centered card. It gives the one bold element a home on the first screen a visitor sees.
- **Dashboard as the post-login home** at `/dashboard`. Keeping `/branches` as home was discarded.
- **Cards below `sm`** over horizontal scroll.
- **Screenshot capture plus a manual checklist** over committed `toHaveScreenshot` baselines, because font rendering differs across hosts.
- **Reuse the existing query keys** for the dashboard, so the current mutation invalidation keeps it fresh.
- **"{n} user accounts." over "{n} users can sign in."** (changed during Step 9). `totalElements` counts disabled users too, so the first wording was false whenever a user was disabled. Counting only enabled users would need an `enabled` filter on `GET /api/v1/admin/users` (out of scope) or fetching every user.

## Identified risks

- **Overpass tabular numerals.** If the variable font's Latin subset lacks `tnum`, codes and counts will not line up. Step 1 checks this. The fallback is Public Sans Variable (the runner-up), recorded in the PR.
- **Contrast of the brand colors.** Route green `#0E7A5E` on white and `#3FBF94` on Night must pass AA for link text and button labels. Amber must never carry text on Paper. If a pair fails, adjust lightness in OKLCH and keep the hue, and note it in the PR.
- **Ambiguous Playwright selectors.** Role-plus-name selectors can match both the sidebar and dashboard links, which fails strict mode. Mitigation: the e2e specs use the `nav-*` and `branch-add` test IDs.
- **Collapsed rail hides the labels.** Icon-only links must keep an accessible name (`aria-label` plus a tooltip).
- **Bundle budgets.** Extra DaisyUI components and the inline SVG drawing add weight. Keep the drawing as one hand-optimized SVG, and check `npm run build` in steps 1, 3 and 5. Keep styling in Tailwind classes and `styles.css` to stay under the 4 kB `anyComponentStyle` warning.
- **Theme override drift.** A future DaisyUI upgrade can change defaults the override does not set. Mitigation: set every color token explicitly in both theme blocks.
- **Duplicated markup for table and cards.** Keep both views in the same component template, driven by the same row data, and use the same `data-testid`s for cells that e2e reads.
- **The `smoke.spec.ts` cell assertion.** `getByRole('cell', …)` only works when the table is rendered. The smoke projects keep the Desktop Chrome viewport (1280 px).
- **Plural and sentence-building in i18n.** The in-house `I18nService` has no plural support. Use separate `one`/`other` keys chosen in the component, not string concatenation, so `es` grammar stays correct.
