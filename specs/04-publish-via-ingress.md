# SPEC 04 — Publish Branch Hub through the shared ingress

> **Status:** Implemented
> **Depends on:** SPEC 01
> **Related:** `jotaeme-ingress` SPEC 01 (shared edge ingress and server conventions, `/mnt/data/apps/jotaeme-ingress/specs/01-shared-edge-ingress.md`), SPEC 05 (public demo access, builds on this one)
> **Date:** 2026-11-11
> **Objective:** Bind every Branch Hub host port to `127.0.0.1` inside its block 18090–18099, run the server stack on the prod overlay, and publish the SPA (with Swagger under the same host) at `code-branchhub.jotaeme.dev` and an anonymous read-only Grafana at `code-branchhub-grafana.jotaeme.dev` through the `server-tuf` tunnel.

## Why this spec exists

`jotaeme-ingress` SPEC 01 moved `cloudflared` into its own stack on the external `edge` network and set the server conventions (`/mnt/data/apps/jotaeme-ingress/docs/conventions.md`). It left Branch Hub out of scope "for a Branch Hub spec that follows these conventions". This is that spec.

Current state (2026-11-11):

- Branch Hub publishes eight host ports on `0.0.0.0`: 18090 (web, through `.env`), 18091 (API, through `.env`), 5433 (Postgres), 9092 (Kafka), 9090 (Prometheus), 9093 (Alertmanager), 9115 (blackbox) and 3000 (Grafana). Any LAN device can reach the database and the broker, and 3000/9090 will clash with future projects.
- The server runs the dev compose with `.env`: actuator public, refresh cookie not `Secure`, Grafana with an admin login.
- `nginx.conf` sends `X-Forwarded-Proto $scheme`. Behind `cloudflared` that is always `http`, so Spring would build `http://` URLs for a site served over `https://`.
- `nginx.conf` proxies all of `/actuator/` to the public host, and proxies `= /swagger-ui.html` and `/v3/` but not `/swagger-ui/**`. The Swagger assets fall into the static-asset regex location and return 404, so Swagger does not load through `web` today.
- The `prod` profile exposes only `health,info`, so on the prod overlay Prometheus cannot scrape `/actuator/prometheus` and the Grafana dashboard would be empty. It also turns Swagger off.
- `.env.prod.example` points Alertmanager at the committed `alertmanager.yml`, which has a placeholder inline webhook URL. The local `alertmanager.local.yml` (gitignored) already reads the real webhook from the `discord-webhook-url` file.

## Target topology

```text
Browser ─https─> Cloudflare ─tunnel server-tuf─> cloudflared (edge)
   code-branchhub.jotaeme.dev          → http://branch-hub-web:80       (SPA, /api, /swagger-ui, /v3, /actuator/health)
   code-branchhub-grafana.jotaeme.dev  → http://branch-hub-grafana:3000 (anonymous Viewer)

branch-hub_default (private, by container/service name):
   web → app:8080 → postgres:5432, kafka:9092
   prometheus → app:8080 (/actuator/prometheus), blackbox:9115 → grafana, kafka, postgres
   prometheus → alertmanager:9093 → Discord
```

- `branch-hub-web` and `branch-hub-grafana` join `default` + `edge`. Every other container stays only on `default`.
- Public traffic never uses a host port. Host ports exist only for the owner, on the server or through VS Code SSH forwarding.

## Scope

**In:**

- Host ports in the base `docker-compose.yml`, all bound to `127.0.0.1`, with the block as defaults:

  | Host port | Variable | Container | Use |
  | --- | --- | --- | --- |
  | 18090 | `WEB_PORT` | `branch-hub-web:80` | The public site without Cloudflare |
  | 18091 | `API_PORT` | `branch-hub-api:8080` | Direct Swagger, actuator, `openapi:generate`, `npm start` proxy |
  | 18092 | `GRAFANA_PORT` | `branch-hub-grafana:3000` | Local dashboard |
  | 18093 | `PROMETHEUS_PORT` | `branch-hub-prometheus:9090` | Queries, targets, rules |
  | 18094 | `ALERTMANAGER_PORT` | `branch-hub-alertmanager:9093` | Silences |
  | 18095 | `POSTGRES_PORT` | `branch-hub-postgres:5432` | psql/DBeaver, `bootRun` from the host |
  | none | | `branch-hub-kafka` | It advertises `kafka:9092`, so a host port does not work from the host anyway |
  | none | | `branch-hub-blackbox` | Only Prometheus calls it |
  | 18096–18099 | | | Free, reserved for Branch Hub |

- **One stack on the server, the published one.** The server's `.env` holds the prod values plus `COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml`, so a plain `docker compose up -d` always deploys the prod overlay and can never replace the demo with dev settings. Day-to-day dev uses `npm start` and `./gradlew bootRun` against the stack's host ports.
- `docker-compose.prod.yml` (prod overlay) gains the publishing settings:
  - `web` and `grafana` join the external `edge` network (and keep `default`).
  - `app`: `SECURITY_COOKIES_SECURE=true`, Swagger and API docs on (`SPRINGDOC_API_DOCS_ENABLED=true`, `SPRINGDOC_SWAGGER_UI_ENABLED=true`), actuator exposure `health,info,prometheus` and `APP_SECURITY_ACTUATOR_PUBLIC=true`, so Prometheus can scrape over the private network. It stays unreachable from outside because the API is not on `edge` and nginx no longer proxies it (next item).
  - `grafana`: anonymous access with the `Viewer` role, login form disabled, sign-up disabled, `GF_SERVER_ROOT_URL` set to the public Grafana URL.
- `frontend/nginx.conf` (shared by dev and prod, baked into the image):
  - Forward the original scheme: a `map` that uses `$http_x_forwarded_proto` when present and falls back to `$scheme`, used by every `proxy_set_header X-Forwarded-Proto`.
  - Replace `location /actuator/` with `location = /actuator/health`. Every other `/actuator/*` path returns 404 from nginx.
  - Add `location ^~ /swagger-ui/` proxied to `app:8080`, so Swagger assets load through `web`.
- Alertmanager: the committed `monitoring/alertmanager/alertmanager.yml` reads the webhook from `webhook_url_file: /run/secrets/discord_webhook_url`, like the local copy, so the prod overlay sends real Discord alerts.
- Env templates: `.env.example` (dev, new port variables with block defaults) and `.env.prod.example` (prod, with `COMPOSE_FILE`, `SECURITY_COOKIES_SECURE=true`, `GRAFANA_PUBLIC_URL`, the Discord file, block ports).
- Cutover on the server, done by the owner: a fresh Postgres volume (`docker compose down -v` once), then the prod overlay up.
- Cloudflare dashboard, done by the owner: two public hostnames on tunnel `server-tuf`, both public with no Access.
- Default ports in dev tooling move to the block: `frontend/proxy.conf.json` targets `http://localhost:18091` (the compose API), `frontend/playwright.config.ts` defaults to `http://localhost:18090`, `frontend/scripts/generate-openapi.mjs` defaults to `http://localhost:18091/v3/api-docs`, and the Postman `baseUrl` defaults to `http://localhost:18091`.
- Docs: `README.md` (public URLs), `docs/getting-started.md` (ports, prod overlay as the server mode, `edge` prerequisite, `DB_URL` with 18095), `docs/security.md` (what is public and why), `backend/README.md`, `frontend/README.md`, `CLAUDE.md` (ports, nginx proxy list, dev-against-stack workflow), `docs/roadmap/status.md`, `CHANGELOG.md`.
- `jotaeme-ingress` repo: `REGISTRY.md` gets Branch Hub's final hostnames, networks, `edge` members and host ports. The owner commits it there.

**Out of scope (for future specs):**

- Public demo access: demo account, `GET /api/v1/public/demo`, login demo card with links, sample branches, nightly reset. That is SPEC 05. Until it ships, visitors see the login screen, Swagger and Grafana, and only the owner can log in.
- Rate limiting, Cloudflare WAF rules, bot protection.
- A second parallel stack on the server (a dev stack next to the demo).
- A separate management port for actuator.
- Monitoring the tunnel or probing the public hostnames from Prometheus.
- Running `bootRun` from the host with a working Kafka (Kafka gets no host port).
- CI/CD or automatic deploys.

## Data model

No new application data and no API contract change. The changes are configuration only.

### Base `docker-compose.yml` ports (target shape)

```yaml
services:
  postgres:
    ports:
      - "127.0.0.1:${POSTGRES_PORT:-18095}:5432"
  kafka:
    # no ports
  app:
    ports:
      - "127.0.0.1:${API_PORT:-18091}:8080"
  web:
    ports:
      - "127.0.0.1:${WEB_PORT:-18090}:80"
  prometheus:
    ports:
      - "127.0.0.1:${PROMETHEUS_PORT:-18093}:9090"
  blackbox:
    # no ports
  alertmanager:
    ports:
      - "127.0.0.1:${ALERTMANAGER_PORT:-18094}:9093"
  grafana:
    ports:
      - "127.0.0.1:${GRAFANA_PORT:-18092}:3000"
```

### `docker-compose.prod.yml` additions (target shape)

```yaml
services:
  app:
    environment:
      SPRING_PROFILES_ACTIVE: docker,prod
      APP_SECURITY_ACTUATOR_PUBLIC: "true"          # private network only; nginx blocks it publicly
      MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health,info,prometheus
      SECURITY_COOKIES_SECURE: "true"
      SPRINGDOC_API_DOCS_ENABLED: "true"
      SPRINGDOC_SWAGGER_UI_ENABLED: "true"

  web:
    networks: [default, edge]

  grafana:
    environment:
      GF_SERVER_ROOT_URL: ${GRAFANA_PUBLIC_URL:?GRAFANA_PUBLIC_URL is required in .env.prod}
      GF_AUTH_ANONYMOUS_ENABLED: "true"
      GF_AUTH_ANONYMOUS_ORG_ROLE: Viewer
      GF_AUTH_DISABLE_LOGIN_FORM: "true"
      GF_USERS_ALLOW_SIGN_UP: "false"
      # GF_SECURITY_ADMIN_USER / _PASSWORD stay required, as today
    networks: [default, edge]

networks:
  edge:
    external: true
```

### `.env.prod.example` additions

```dotenv
# Plain `docker compose ...` on the server always uses the prod overlay
COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml

ALERTMANAGER_CONFIG_FILE=./monitoring/alertmanager/alertmanager.yml
DISCORD_WEBHOOK_FILE=./monitoring/alertmanager/discord-webhook-url
SECURITY_COOKIES_SECURE=true
GRAFANA_PUBLIC_URL=https://code-branchhub-grafana.jotaeme.dev
```

The `WEB_PORT=8080` / `API_PORT=8081` lines are removed from both templates. The compose defaults are the block.

### `nginx.conf` (target shape of the changed parts)

```nginx
map $http_x_forwarded_proto $forwarded_proto {
    default $http_x_forwarded_proto;
    ""      $scheme;
}

server {
    # ... every proxied location:
    proxy_set_header X-Forwarded-Proto $forwarded_proto;

    location ^~ /swagger-ui/      { proxy_pass http://app:8080; ... }
    location = /actuator/health   { proxy_pass http://app:8080; ... }
    location ^~ /actuator/        { return 404; }   # replaces the old catch-all proxy
}
```

### Cloudflare public hostnames (tunnel `server-tuf`, set by the owner)

| Hostname | Service | Access |
| --- | --- | --- |
| `code-branchhub.jotaeme.dev` | `HTTP branch-hub-web:80` | Public |
| `code-branchhub-grafana.jotaeme.dev` | `HTTP branch-hub-grafana:3000` | Public, Grafana anonymous Viewer |

## Implementation plan

Steps marked *(owner)* are run by the owner. Claude proposes the exact commands. Each step leaves the stack working.

1. **Ports in the base compose.** Bind every remaining port to `127.0.0.1` with the block defaults, remove the Kafka and blackbox ports, update `.env.example`. Check: `docker compose config` shows `host_ip: 127.0.0.1` on all six ports and no others.
2. **nginx.** Add the forwarded-proto `map`, `location ^~ /swagger-ui/`, `location = /actuator/health` and the explicit 404 for other actuator paths. Check: `npm run build` passes. Locally (dev compose) `http://127.0.0.1:18090/swagger-ui.html` loads, `/actuator/health` returns 200 and `/actuator/prometheus` returns 404.
3. **Prod overlay.** Add the `app`, `web` and `grafana` settings and the external `edge` network to `docker-compose.prod.yml`. Update `.env.prod.example`. Switch `alertmanager.yml` to `webhook_url_file`. Check: `docker compose --env-file .env.prod.example -f docker-compose.yml -f docker-compose.prod.yml config` is valid (dummy secrets) and lists `edge` only on `web` and `grafana`.
4. **Dev tooling defaults.** `proxy.conf.json`, `playwright.config.ts`, `generate-openapi.mjs`, Postman `baseUrl`. Check: `npm test` passes, and the Postman JSON is still valid.
5. **Docs.** Update the files listed in Scope. Check: `grep -rnE "5433|:3000\b|:9090|:9093|:8081|localhost:8080"` over the docs and tooling finds only the intended references (in-container ports, historical specs).
6. *(owner)* **Server `.env` for prod.** Back up the current `.env` as `.env.dev.bak`. Write the new `.env` from `.env.prod.example` with real secrets (`JWT_SECRET`, `APP_SUPER_ADMIN_PASSWORD`, `POSTGRES_PASSWORD`/`DB_PASSWORD`, `GF_SECURITY_ADMIN_PASSWORD`) and `COMPOSE_FILE`. Check: `docker compose config` shows the prod overlay values (profiles `docker,prod`, `edge` on `web`/`grafana`).
7. *(owner)* **Cutover, fresh volume.** From the repo root:

   ```bash
   docker compose down -v          # removes the dev data volume once, on purpose
   docker compose up -d --build    # prod overlay through COMPOSE_FILE
   docker compose ps               # every service healthy
   docker network inspect edge --format '{{range .Containers}}{{.Name}} {{end}}'
   ```

   Check: `edge` lists `cloudflared`, `n8n`, `branch-hub-web`, `branch-hub-grafana` and nothing else. `ss -ltnp` shows Branch Hub listeners only on `127.0.0.1:18090–18095`.
8. *(owner)* **Cloudflare.** Add the two public hostnames on `server-tuf` (see `jotaeme-ingress` `docs/operations.md`, "Add a public hostname"). Check: both hostnames load over HTTPS.
9. **Verify end to end** against the acceptance criteria below, including one Discord test alert.
10. **Registry and close.** Update `jotaeme-ingress/REGISTRY.md` (stack row, containers and networks, public hostnames, `edge` members, host ports). Update `docs/roadmap/status.md` and `CHANGELOG.md` here. Propose the git commands for both repos.

**Rollback** (until step 10): `docker compose down`, restore `.env.dev.bak` as `.env`, `docker compose up -d --build` (dev mode on the new 127.0.0.1 ports, empty database), and remove the two hostnames in Cloudflare.

## Acceptance criteria

- [x] `docker compose config` (server `.env`) shows exactly six host ports, 18090–18095, each with `host_ip: 127.0.0.1`. Kafka and blackbox have none.
- [x] `ss -ltnp` on the server shows no Branch Hub listener on `0.0.0.0` or `[::]`, and nothing listening on 5433, 9092, 9090, 9093, 9115 or 3000 from Branch Hub.
- [x] `docker network inspect edge` lists exactly `cloudflared`, `n8n`, `branch-hub-web` and `branch-hub-grafana`.
- [x] A plain `docker compose up -d` on the server deploys the prod overlay (`SPRING_PROFILES_ACTIVE=docker,prod` in `docker inspect branch-hub-api`).
- [x] `https://code-branchhub.jotaeme.dev` loads the SPA. A SUPER_ADMIN login works, and a page reload keeps the session (refresh through the cookie).
- [x] The `REFRESH_TOKEN` cookie set on `https://code-branchhub.jotaeme.dev` has the `Secure` and `HttpOnly` flags.
- [x] `https://code-branchhub.jotaeme.dev/swagger-ui.html` loads with every asset (no 404 in the network tab). Its server URL is `https://code-branchhub.jotaeme.dev`, and "Try it out" on `POST /api/v1/auth/login` returns 200.
- [x] `https://code-branchhub.jotaeme.dev/actuator/health` returns 200. `/actuator/prometheus`, `/actuator/metrics` and `/actuator/env` return 404.
- [x] `https://code-branchhub-grafana.jotaeme.dev` opens the Branch Hub dashboard with no login. `/login` shows no login form, and the anonymous user cannot edit or save a dashboard.
- [x] `http://127.0.0.1:18093/targets` shows `branch-hub-api` and both blackbox jobs `UP`, and the Grafana dashboard shows `app_*` series.
- [x] A test alert reaches the Discord channel from the prod overlay (stop `branch-hub-api` for more than 1 minute, or post a test alert with `amtool`), and the resolved message follows.
- [x] `./gradlew clean test`, `npm test` and `npm run build` pass.
- [x] No secret in any committed file. `.env` and `.env.prod` stay ignored, and `.env.prod.example` holds placeholders only.
- [x] `jotaeme-ingress/REGISTRY.md` lists Branch Hub's hostnames, `edge` members and the six `127.0.0.1` ports. The "to be fixed in its own spec" notes are gone.
- [x] `README.md`, `docs/getting-started.md`, `docs/security.md`, `CLAUDE.md`, `docs/roadmap/status.md` and `CHANGELOG.md` reflect the new ports and the published URLs.

## Decisions taken and discarded

- **Swagger under the SPA host** (`/swagger-ui.html` through nginx) over its own hostname (`code-branchhub-api`). Only `web` joins `edge`, the API stays private, and same-origin means "Try it out" works with no CORS. Not publishing Swagger was discarded because the project exists to show the code.
- **Grafana on its own hostname**, over a `/grafana` path on the SPA host. Grafana is a third-party app. On the SPA's origin, an XSS in Grafana could call `/api/v1/auth/refresh` with the visitor's refresh cookie, and the SPA's strict CSP would need overrides for Grafana's inline scripts. A sub-path also needs `serve_from_sub_path` and a websocket proxy. It also matches the example in the ingress conventions.
- **Grafana anonymous Viewer with the login form disabled**, over Cloudflare Access (visitors could not see it) or not publishing it (screenshots only). Dashboards are provisioned from files, so no admin UI is needed. The admin account stays configured for `grafana-cli` through `docker exec`.
- **Prod overlay on the server**, over the dev `.env` with port changes. Dev defaults (public actuator, non-`Secure` cookie) must not be published.
- **Publishing settings in `docker-compose.prod.yml`, not the base compose.** Someone who clones the public repo can run the dev compose without creating the external `edge` network.
- **`COMPOSE_FILE` in the server `.env`** (Claude's choice, to support "one stack = the demo"). A plain `docker compose up` cannot deploy dev settings over the public demo, and the long `--env-file ... -f ... -f ...` command is no longer needed on the server.
- **One stack on the server, the demo**, over two parallel stacks. Fixed container names and one compose project make parallel stacks a bigger change. Dev uses `npm start` and `bootRun` against the stack's host ports.
- **nginx limits `/actuator` to `health`; the API exposes `prometheus` on the private network**, over a separate management port (it changes Spring Security and health-check wiring) or no metrics in prod (empty public dashboard). Config only, no Java change.
- **Six debug host ports in the block, in the base compose.** One port per tool the owner opens by hand. Kafka gets none because its advertised listener `kafka:9092` does not resolve from the host. Blackbox gets none because only Prometheus uses it.
- **Fresh Postgres volume at cutover** over `ALTER USER` on the dev volume. The data is demo data, and a fresh start runs Flyway V1 and the seeders under the new secrets.
- **Real Discord alerts in prod through the webhook file**, like local. The owner hears when the public demo is down.
- **Split into SPEC 04 (infra and publishing) and SPEC 05 (demo access).** The full feature touched five areas. 04 is configuration only and can ship alone.

## Identified risks

- **Wrong forwarded scheme.** If `cloudflared` does not send `X-Forwarded-Proto: https`, Spring still sees `http`. The `Secure` cookie is still sent by the browser over HTTPS, but Swagger's server URL and any absolute links would be `http://`. Step 9 checks the Swagger server URL. The fallback is to hard-code `https` in the prod nginx through an env-templated config.
- **`Secure` cookie over plain HTTP.** On the prod overlay, `http://127.0.0.1:18090` works in Chromium and Firefox because they treat `localhost`/`127.0.0.1` as a secure context. Any other plain-HTTP origin (a LAN IP) cannot keep the refresh cookie. Documented in `docs/getting-started.md`.
- **`npm start` against the prod API.** `ng serve` on `http://localhost:4200` proxies to 18091. Same note as above: it works because of the `localhost` exemption.
- **Public login with no rate limit.** The SUPER_ADMIN login is reachable from the internet. Mitigation: a strong `APP_SUPER_ADMIN_PASSWORD`, and rate limiting in a future spec. Cloudflare's default protections apply meanwhile.
- **Grafana anonymous data leak.** Anyone can read every panel, including Postgres and Kafka up/down data and request rates. No secrets are in those metrics, but new panels must keep it that way.
- **Alert noise during rebuilds.** A `docker compose up -d --build` that keeps the API down for over a minute fires `BranchHubApiDown` to Discord. Accepted. Silence it in Alertmanager (18094) for planned work.
- **Destructive cutover.** `docker compose down -v` deletes the current database. That is intended, but it must run in the Branch Hub folder only. Step 7 gives the exact command.
