# SPEC 01 — Rename to branch-hub and get the repo ready to publish

> **Status:** Implemented
> **Depends on:** none in this repo. Related: `jotaeme-dev` SPEC 05 (Draft), which lists this project in the `code` category and says the repo cleanup comes before `/code`.
> **Date:** 2026-11-04
> **Objective:** Rename `spring-web` to `branch-hub` across code, infra and GitHub, and turn its README, docs and repo metadata into a recruiter-ready portfolio showcase that links to `jotaeme.dev`.

## Why this spec exists

The owner wants to publish this project from `jotaeme.dev` (the `code` category planned in `jotaeme-dev` SPEC 05). The personal-branding repo (`PersonalBranding/deuda-tecnica.md`) puts `spring-web` first in the GitHub review: clear README, description, topics, and proof that it runs. Today the repo has these problems:

- The name `spring-web` says the framework, not what the app does.
- The root README calls the project a "base template for forks", signs as "JuanMa — Backend Java Developer", uses the retired LinkedIn URL `/juanmavelezdev` (decision 005 of the branding repo), shows the personal email, has a Spanish line, and never links to `jotaeme.dev`.
- There is no license, no architecture picture and no screenshots. The GitHub "Website" field is empty.
- The docs mix project documentation with chat-era notes ("You do not need a fresh chat window", the git-branch vs IAM-code confusion table).

The owner decided to rename everything, including the Java package and the database, and accepted the extra work.

## Scope

**In:**

- **Rename to `branch-hub`** everywhere in this repo. The full map is in "Data model":
  - Java base package `com.jm.spring_web` → `dev.jotaeme.branchhub` (main and test sources).
  - Application classes, the Gradle project, `spring.application.name`, logging keys and the logback default.
  - Database name and user (`companydb` / `company`) in `.env.example`, `.env.prod.example`, Compose defaults and healthcheck, and the `application*.properties` defaults.
  - Compose container names, a top-level Compose project `name`, the Prometheus job and alert group, the Grafana dashboard file, uid, tags and provider name.
  - The frontend package name, the SPA `<title>`, the preference storage prefix (`preference-keys.ts` and `public/theme-init.js`, kept in sync), and the Postman collection file and name.
- **Health pass:** run the backend tests, frontend tests and build, and the e2e smoke suites before and after the rename. Fix only what fails or is inconsistent. No new features.
- **Root cleanup and docs reorganization:**
  - New `LICENSE` (MIT, `Copyright (c) 2026 Juan Manuel Vélez Parra`).
  - A lean, recruiter-first root `README.md` (structure in "Data model").
  - New `docs/architecture.md` with Mermaid diagrams.
  - New `docs/getting-started.md`, which takes the detailed run instructions out of the root README.
  - New `docs/images/` with four screenshots.
  - `docs/README.md` rewritten as an index.
- **Roadmap docs kept and rewritten as project docs:** `docs/roadmap/*.md` and `CHANGELOG.md` drop the "base template / forks" framing and the chat-style notes. The waves, IAM codes and status stay. Overlapping files are merged only where the content is repeated.
- **Positioning:** a portfolio showcase. `v1.x` stays as release numbering of this project, not "template maturity".
- **Author block:** "Juan Manuel Vélez Parra — Java Backend Engineer" (decision 002), with links to `https://jotaeme.dev`, `https://www.linkedin.com/in/juanmavelezpa` and `https://github.com/JuanMaVelezPa`. No email.
- **Module docs and agent docs:** `backend/README.md`, `frontend/README.md` and `CLAUDE.md` updated for the new names and paths.
- **GitHub metadata, applied by the owner** with the commands this spec gives: rename the repo, update the git remote, set the description, set the Website to `https://jotaeme.dev`, and add topics.

**Out of scope (for future specs):**

- Fixing Flyway. The baseline health pass found that Flyway never runs: Spring Boot 4 needs `spring-boot-starter-flyway`, and the `docker` profile hides it with `spring.jpa.hibernate.ddl-auto=update`. The fix touches several files (H2-based tests would run the Postgres scripts), so it goes in its own spec, which is the next one for this repo (SPEC 02).
- CI with GitHub Actions and a status badge. This goes in its own spec, after the Flyway fix.
- A live demo (for example `branch-hub.jotaeme.dev` through Cloudflare Tunnel). It needs a demo user, data reset and a hardened profile.
- Any change in `jotaeme-dev` (the `/code` page, `projects.json`) or in `PersonalBranding` (`branding/github.md`, `deuda-tecnica.md`, the decision 002 text, the GitHub profile README). They are listed under "Follow-ups in other repos".
- Renaming the local folder `/mnt/data/apps/spring-web`.
- Kafka topic and consumer-group names (`hr-new-branch-notifications`, `rrhh-notifier`). They describe the HR notification domain, not the app name.
- Metric names (`app_*`). Alerts depend on them, and they do not contain the app name.
- Migrating old `spring-web.pref.*` browser preferences to the new prefix.
- New features, refactors outside the rename, and fixes that need more than a small change. A failure that needs a feature-size fix stops the spec and becomes a new spec.
- A GIF walkthrough, and any AI or Spec-Driven Development wording in the README (decision 010 of the branding repo allows it only in the GitHub profile README, LinkedIn and the site).

## Data model

This spec introduces no runtime data structures. It renames identifiers and adds documents. The rename map below is the contract.

### Rename map

| Where | Before | After |
| ----- | ------ | ----- |
| GitHub repo | `JuanMaVelezPa/spring-web` | `JuanMaVelezPa/branch-hub` |
| Java base package (main and test) | `com.jm.spring_web` | `dev.jotaeme.branchhub` |
| Source folders | `backend/src/{main,test}/java/com/jm/spring_web/` | `backend/src/{main,test}/java/dev/jotaeme/branchhub/` |
| Main class | `SpringWebApplication` | `BranchHubApplication` |
| Context test | `SpringWebApplicationTests` | `BranchHubApplicationTests` |
| `ServletInitializer` | references `SpringWebApplication` | references `BranchHubApplication` |
| `backend/settings.gradle` | `rootProject.name = 'spring-web'` | `rootProject.name = 'branch-hub'` |
| `backend/build.gradle` group | `com.jm` | `dev.jotaeme` |
| Boot jar in `backend/Dockerfile` | `spring-web-0.0.1-SNAPSHOT.jar` | `branch-hub-0.0.1-SNAPSHOT.jar` |
| Problem Details `type` base URI | `https://api.spring-web/errors/` | `https://api.branch-hub/errors/` |
| `spring.application.name` | `spring-web` | `branch-hub` |
| `logback-spring.xml` default `APP_NAME` | `spring-web` | `branch-hub` |
| `logging.level.*` keys | `com.jm.spring_web…` | `dev.jotaeme.branchhub…` |
| DB name / user / dev password | `companydb` / `company` / `company` | `branchhub` / `branchhub` / `branchhub` |
| `DB_URL` default | `…/companydb` | `…/branchhub` |
| Compose project | (folder name) | `name: branch-hub` |
| Containers | `company-postgres`, `company-kafka`, `branches-api`, `company-prometheus`, `company-blackbox`, `company-alertmanager`, `company-grafana` (+ the `web` container if it has a name) | `branch-hub-postgres`, `branch-hub-kafka`, `branch-hub-api`, `branch-hub-prometheus`, `branch-hub-blackbox`, `branch-hub-alertmanager`, `branch-hub-grafana` (`branch-hub-web`) |
| Prometheus job | `spring-web-app` | `branch-hub-api` |
| Alert group | `spring-web-alerts` | `branch-hub-alerts` |
| Alert expressions and descriptions | `job="spring-web-app"`, "spring-web app" | `job="branch-hub-api"`, "branch-hub API" |
| Grafana dashboard file | `spring-web-observability.json` | `branch-hub-observability.json` |
| Grafana uid / tags / provider | `spring-web-observability` / `spring-web` / `spring-web-dashboards` | `branch-hub-observability` / `branch-hub` / `branch-hub-dashboards` |
| `frontend/package.json` name | `frontend` | `branch-hub-web` |
| SPA `<title>` | `Branches` | `Branch Hub` |
| Preference prefix | `spring-web.pref.v1` | `branch-hub.pref.v1` |
| Postman file / collection name | `spring-web.postman_collection.json` / `spring-web API` | `branch-hub.postman_collection.json` / `branch-hub API` |
| Display name in docs | Spring Web | Branch Hub |

### GitHub metadata

```text
name:        branch-hub
description: Full-stack branch management app. Spring Boot 4 API (Java 25) with Clean Architecture, JWT auth and a Kafka transactional outbox, an Angular 21 SPA, and Prometheus/Grafana monitoring.
website:     https://jotaeme.dev
topics:      keep the current 14 + angular, typescript, postgresql, flyway   (18 total, limit 20)
```

### Folder structure after the spec

```text
branch-hub/
├── LICENSE                         NEW: MIT
├── README.md                       REWRITTEN: recruiter-first showcase
├── CHANGELOG.md                    CHANGED: "template" wording removed, new "Unreleased" entry for the rename
├── CLAUDE.md                       CHANGED: names, package, paths
├── .env.example, .env.prod.example CHANGED: DB name and user
├── docker-compose.yml              CHANGED: project name, container names, DB defaults
├── docker-compose.prod.yml         CHANGED only if it repeats a renamed value
├── backend/                        CHANGED: package move, names, README
├── frontend/                       CHANGED: package name, title, storage prefix, README
├── monitoring/                     CHANGED: job, alerts, dashboard file and uid
├── specs/01-branch-hub-publish-readiness.md   this spec
└── docs/
    ├── README.md                   REWRITTEN: index
    ├── architecture.md             NEW: Mermaid diagrams and key flows
    ├── getting-started.md          NEW: run with Compose or locally, ports, DB URLs, default login, troubleshooting
    ├── security.md                 CHANGED: names only
    ├── images/                     NEW: login.png, branches.png, admin-users.png, grafana.png
    ├── postman/branch-hub.postman_collection.json   RENAMED
    └── roadmap/*.md                REWRITTEN as project docs
```

### Root README structure

1. `# Branch Hub`, then a one-line tagline in plain B1 English, for example: "A full-stack app to manage company branches, built like a production system: Spring Boot 4 + Angular 21, secure by default, observable end to end."
2. Highlights: 5–7 bullets on what a recruiter should notice (Clean Architecture, JWT with an HttpOnly refresh cookie and role-based access, transactional outbox with an idempotent Kafka consumer, RFC 7807 errors, Flyway, Testcontainers tests, Prometheus/Grafana/Alertmanager with Discord alerts, a typed OpenAPI contract for the SPA).
3. Screenshots: the four images in `docs/images/`.
4. Architecture: one Mermaid system diagram, with a link to `docs/architecture.md`.
5. Tech stack: a short table (backend, frontend, data and messaging, observability, testing).
6. Quick start: at most 6 lines with `cp .env.example .env` and `docker compose up -d --build`, then the URLs, with a link to `docs/getting-started.md`.
7. Project structure: a short tree.
8. Documentation: links to the module READMEs and `docs/`.
9. Roadmap: one line and a link to `docs/roadmap/status.md`.
10. Author block (as in Scope) and `License: MIT`.

`docs/architecture.md` holds three Mermaid diagrams: the system view (browser → nginx `web` → `app` → Postgres / Kafka; Prometheus, Blackbox, Alertmanager → Discord, Grafana), the backend layers (`domain` ← `application` ← `infrastructure` / `entrypoints`), and the branch-write outbox sequence (use case → `branch` + `outbox_event` in one transaction → relay → Kafka → idempotent consumer with `processed_event`, retries and DLT).

Screenshot conventions: the PNGs come from the running Compose stack at 1440×900, with the light theme and the English UI. They use fictional sample data, such as made-up branch codes and cities, plus the default `admin@example.com` user. No real names or emails. Each file is under 500 KB.

## Implementation plan

0. **Precondition (owner):** commit or stash the current work on `main` (`.env.example` and `docker-compose.yml` port variables, the untracked `CLAUDE.md`). Then create the spec branch. The working tree must be clean.
1. **Baseline health pass.** Run `./gradlew clean test` (from `backend/`), then `npm ci && npm test && npm run build` (from `frontend/`), then `docker compose up -d --build` with `npm run test:e2e:smoke` and `npm run test:e2e:admin-smoke`. Write the results into the PR notes. Fix only the failures, each one as a small change. If a failure needs more than a small fix, stop and ask.
2. **Java package and app names.** `git mv` the main and test trees to `dev/jotaeme/branchhub/`. Rewrite the `package` and `import` lines. Rename the main class and the context test. Update `ServletInitializer`, `settings.gradle`, `spring.application.name`, the `logging.level.*` keys in every `application*.properties` (main and test) and the logback default. Verify: `grep -rn 'com.jm' backend/src` returns nothing, and `./gradlew clean test` passes.
3. **Database name and user.** Change `companydb` / `company` to `branchhub` in `.env.example`, `.env.prod.example`, the `docker-compose*.yml` defaults and healthcheck, the `application*.properties` defaults and any test that references them. The owner updates their own `.env` and runs `docker compose down -v`. Verify: `./gradlew clean test` passes, the stack starts from an empty volume, the database `branchhub` has the app tables, and login with the `.env` super admin works. (Checking that Flyway applies V1–V4 moved to SPEC 02, because Flyway does not run yet.)
4. **Infra labels.** Add `name: branch-hub` to the Compose file. Rename the containers. Rename the Prometheus job and alert group in `prometheus.yml` and `alerts.yml`. Rename the Grafana dashboard file, uid, tags and provider. Verify: `docker compose ps` lists only `branch-hub-*` containers, all healthy. Prometheus shows the `branch-hub-api` target as UP and the `branch-hub-alerts` rules as loaded. Grafana opens the dashboard by its new uid.
5. **Frontend names.** Change the `package.json` name (and the lockfile name field), the `<title>`, and the preference prefix in `preference-keys.ts` and `public/theme-init.js`, with the comment that links them. If the i18n service uses the old name in a key or a label, rename it. Verify: `npm test` and `npm run build` pass, the theme toggle persists after a reload, and both e2e smoke suites pass against Compose.
6. **Postman collection.** Rename the file and the collection name, and update every link to it. Verify: the JSON parses (`node -e "JSON.parse(require('fs').readFileSync(...))"`) and no link points to the old file.
7. **License.** Add `LICENSE` (MIT, 2026, Juan Manuel Vélez Parra).
8. **Architecture doc.** Write `docs/architecture.md` with the three Mermaid diagrams and short notes on the outbox, pagination, errors and auth flows (taken from `CLAUDE.md` and `backend/README.md`, not invented).
9. **Getting-started doc.** Move the detailed run content of the root README into `docs/getting-started.md`: Compose, ports (`WEB_PORT`, `API_PORT`), DB URLs inside and outside Compose, default login, the 422 troubleshooting note, and running without Docker. Remove the Spanish line.
10. **Screenshots.** With the stack running, create the fictional sample data through the API. Capture `login.png`, `branches.png`, `admin-users.png` and `grafana.png` with Playwright into `docs/images/`.
11. **Root README.** Rewrite it following "Root README structure".
12. **Docs index, roadmap and changelog.** Rewrite `docs/README.md` as an index. In `docs/roadmap/*.md` and `CHANGELOG.md`, remove the "base template / forks / template maturity" wording, the "fresh chat window" handoff note and the git-branch vs IAM-code confusion table. Merge content only where it repeats. Add an `## Unreleased` entry to `CHANGELOG.md` that describes the rename, the license and the docs. Update `docs/security.md` for the names only.
13. **Module and agent docs.** Update `backend/README.md`, `frontend/README.md` and `CLAUDE.md` with the new names, the package, the DB name, the dashboard, the Postman path and the new docs. In `docs/roadmap/status.md`, record this spec as the last done item.
14. **Final health pass.** Repeat the step 1 commands. Run the residual-name grep and the link check from the acceptance criteria.
15. **GitHub (owner, after the PR is merged).** The spec PR gives the exact commands:
    - `gh repo rename branch-hub`
    - `git remote set-url origin https://github.com/JuanMaVelezPa/branch-hub.git`
    - `gh repo edit --description "…" --homepage https://jotaeme.dev --add-topic angular,typescript,postgresql,flyway`
    - Open the repo page and check that the MIT license, the Website and the topics show, and that the Mermaid diagrams and screenshots render.

## Acceptance criteria

- [x] `grep -rnI -e 'spring-web' -e 'spring_web' -e 'SpringWeb' -e 'Spring Web' -e 'companydb' -e 'com\.jm\.' --exclude-dir={node_modules,build,dist,.angular,.gradle,.git} .` matches only in `specs/` and in `CHANGELOG.md` entries that describe the rename.
- [x] Every Java file under `backend/src` declares a package that starts with `dev.jotaeme.branchhub`. The main class is `BranchHubApplication`.
- [x] `./gradlew clean test` passes (from `backend/`).
- [x] `npm test` and `npm run build` pass (from `frontend/`).
- [x] `npm run test:e2e:smoke` and `npm run test:e2e:admin-smoke` pass against the Compose stack.
- [x] After `docker compose down -v && docker compose up -d --build`, `docker compose ps` shows only `branch-hub-*` containers, all running and healthy, and the database is `branchhub`.
- [x] Login in the SPA works with the `.env` super admin credentials.
- [x] Prometheus `/targets` shows job `branch-hub-api` as UP. `/rules` shows group `branch-hub-alerts`.
- [x] Grafana opens `/d/branch-hub-observability` with data in its panels.
- [x] After a theme change and a reload, the theme persists, and `localStorage` holds keys that start with `branch-hub.pref.v1`.
- [x] `LICENSE` exists with the MIT text and `Copyright (c) 2026 Juan Manuel Vélez Parra`.
- [x] The root `README.md` follows the 10-part structure. It has no Spanish text, no email address, no `juanmavelezdev`, and no "template" or "fork" wording. It links to `https://jotaeme.dev`, `https://www.linkedin.com/in/juanmavelezpa` and `https://github.com/JuanMaVelezPa`.
- [x] `grep -rnI -e 'juanmavelezdev' -e 'gmail.com' --exclude-dir={node_modules,build,dist,.git} .` matches only in `specs/`, where the spec quotes the strings it forbids.
- [x] `docs/architecture.md` and `docs/getting-started.md` exist. `docs/architecture.md` has three Mermaid blocks.
- [x] `docs/images/` has `login.png`, `branches.png`, `admin-users.png` and `grafana.png`. Each one is under 500 KB, is linked from the root README, and shows only fictional data.
- [x] No file under `docs/` or `CHANGELOG.md` contains "base template", "forks" or "fresh chat window".
- [x] Every relative link in the repo's `*.md` files (outside `node_modules`) points to an existing file, checked with `npx markdown-link-check` or an equivalent script.
- [x] On GitHub (after step 15): `github.com/JuanMaVelezPa/branch-hub` loads, and `github.com/JuanMaVelezPa/spring-web` redirects to it. The sidebar shows the MIT license, the Website `https://jotaeme.dev`, the new description and 18 topics. The README Mermaid diagram renders.
- [x] `git remote get-url origin` returns `https://github.com/JuanMaVelezPa/branch-hub.git`.

## Decisions

- **Yes:** rename to `branch-hub`. The owner found `spring-web` too generic. `branch-hub` names the domain in two plain words that are easy to say in an interview. The stack goes in the description and topics.
- **No:** `branchops` (more product-like, less clear) or `spring-angular-branches` (long, and it reads like a tutorial).
- **No (owner overruled):** keeping the slug and only adding a descriptive title. That was the assistant's suggestion because it has zero ripple. The owner accepted the cost of the full rename.
- **Yes:** Java package `dev.jotaeme.branchhub`. It is the reverse of the domain the owner owns, which is the Java convention, and it ties the code to the brand.
- **Yes:** the rename includes the database (`branchhub`) and needs a fresh local volume. It is a dev-only cost, and it leaves no `company*` leftovers.
- **Yes:** add a Compose project `name`. Volume and network names follow the new name, and the step 3 `down -v` already starts from a clean state.
- **No:** renaming Kafka topics, the consumer group or `app_*` metrics. They do not carry the app name, and the alerts depend on the metrics.
- **No:** migrating old browser preference keys. A reset of theme and language is a one-time, harmless cost.
- **Yes:** a portfolio showcase, not a reusable template. A recruiter should read it as a production-style project the owner built. `v1.x` stays as release numbering.
- **Yes:** keep the roadmap public, rewritten as project docs. It shows planning and delivery. The chat-era notes go.
- **Yes:** MIT license. It is short and common for portfolio projects, and without a license nobody may legally reuse the code.
- **Yes:** an author block without email. The canonical LinkedIn URL is used (decision 005), and the title follows decision 002. `jotaeme.dev` already has contact options, and a public email in a repo attracts scraping.
- **Yes:** the GitHub Website is `https://jotaeme.dev` for now. It changes to `https://jotaeme.dev/code` when that page exists (follow-up).
- **Yes:** a Mermaid architecture diagram plus four screenshots as visual proof. Mermaid renders on GitHub and is versioned as text.
- **No:** a GIF. It is heavier and harder to keep current.
- **No:** CI in this spec. The owner wants it as a separate spec, after the Flyway fix (SPEC 02).
- **No:** fixing Flyway in this spec. It needs more than a small change, so the owner moved it to SPEC 02.
- **No:** a live demo. The owner wants to fix and improve the project before publishing anything that runs.
- **Yes:** a "health pass" means running every existing suite before and after the rename and fixing only failures or inconsistencies. No new features.
- **Yes:** this spec edits only this repo. The other two repos own their texts. Their follow-ups are listed below.
- **Yes:** the owner runs the GitHub rename and the metadata commands after merge (repo convention: the owner runs git and outward-facing commands).
- **No:** AI or SDD wording in the README. Decision 010 of the branding repo lists where the method may appear, and a project README is not on that list.
- **Yes:** the spec is written in English, like all docs in this repo (`CLAUDE.md`).

## Risks

| Risk | Mitigation |
| ---- | ---------- |
| The owner's `.env` still has `companydb` / `company`, so the app cannot connect after step 3 | Step 3 includes updating `.env` and `docker compose down -v`. `getting-started.md` says it once. |
| `down -v` deletes local data (branches, users, audit log) | Dev data only. The super admin is seeded again at startup. |
| A missed reference to the old package or Prometheus job makes the app or the alerts fail quietly | Step 4 checks the target and the rules in the Prometheus UI. The residual-name grep is an acceptance criterion. |
| The IDE or the Gradle cache keeps the old package paths | Use `./gradlew clean`, and refresh the IDE project after step 2. |
| Bookmarked Grafana URLs (old uid) and Prometheus alert history break | Local monitoring only. Accepted. |
| The GitHub redirect from `spring-web` stops working if a new repo named `spring-web` is ever created on the account | Never reuse the name. Update the external links listed in the follow-ups instead of relying on the redirect. |
| Screenshots show personal data | Only fictional sample data and `admin@example.com`, checked before commit. |
| The baseline health pass finds a failure that needs a big fix | Stop, record it, and open a separate spec. Do not grow this spec. |

## Follow-ups in other repos (not done here)

- **PersonalBranding:** in `branding/github.md`, change the profile README project link and the pinned-repo list from `spring-web` to `branch-hub`. In `deuda-tecnica.md`, mark the `spring-web` README, description and topics items as done (CI stays pending). Review the `spring-web` mention in decision 002 and the "Java 21+ vs Java 25" item.
- **GitHub profile README (`JuanMaVelezPa/JuanMaVelezPa`):** update the featured-project link and text.
- **jotaeme-dev:** SPEC 05 and the future `/code` spec use `branch-hub` as the project id and repo URL. When `/code` exists, change this repo's Website field to `https://jotaeme.dev/code`.

## What is **not** in this spec

- CI with GitHub Actions and a badge (next spec).
- A live demo or a subdomain.
- Changes in `jotaeme-dev` or `PersonalBranding`.
- Renaming the local folder, Kafka topics, the consumer group or metrics.
- New features or large fixes found during the health pass (for example, the Flyway fix in SPEC 02).
- A GIF, or AI wording in the README.

Each one, if it lands, goes in its own spec.
