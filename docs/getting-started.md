# Getting started

This guide runs Branch Hub on your machine, either as a full Docker Compose stack or with the API and the SPA running locally. Run Docker Compose and git from the repository root, Gradle from `backend/`, and npm from `frontend/`.

## Prerequisites

| Tool | Needed for |
| ---- | ---------- |
| Docker with Compose v2 | The full stack, and the backend integration tests (Testcontainers) |
| JDK 25 | Running or testing the API outside Docker (`./gradlew`) |
| Node.js and npm | Running or testing the SPA outside Docker |

## Run the full stack with Docker Compose

1. Create your local configuration:

   ```bash
   cp .env.example .env
   cp monitoring/alertmanager/alertmanager.local.yml.example monitoring/alertmanager/alertmanager.local.yml
   cp monitoring/alertmanager/discord-webhook-url.example monitoring/alertmanager/discord-webhook-url
   ```

   - In `.env`, set `JWT_SECRET` (at least 32 characters) and `APP_SUPER_ADMIN_PASSWORD`. Both are required.
   - `.env` points Alertmanager to the two local files above. Both are gitignored. Put a Discord webhook URL in `discord-webhook-url` if you want alerts in Discord; without one, alerts still fire, but nothing is delivered.

2. Build and start everything:

   ```bash
   docker compose up -d --build
   docker compose ps          # wait until the services report "healthy"
   ```

   `--build` rebuilds the `app` and `web` images. If you did not change any code, `docker compose up -d` is enough.

3. Open the SPA at `http://127.0.0.1:18090` and log in (see [Default login](#default-login)).

Useful commands:

```bash
docker compose logs -f web app    # follow the SPA and API logs
docker compose down               # stop the stack, keep the data
docker compose down -v            # stop the stack and delete the database volume
```

The Compose project is named `branch-hub`, so containers are `branch-hub-*` and the database volume is `branch-hub_postgres-data`.

## Ports and URLs

Every host port is bound to `127.0.0.1` and sits in the Branch Hub block 18090–18099, so nothing is reachable from the LAN. On a remote server, reach them through SSH port forwarding (VS Code forwards them for you).

| Host port | Variable | Service | Notes |
| --------- | -------- | ------- | ----- |
| 18090 | `WEB_PORT` | SPA (`web`, Nginx) | Browser entry point. Nginx proxies `/api`, `/swagger-ui`, `/v3/*` and `/actuator/health` to `app`, so the browser stays same-origin and needs no CORS setup. Every other `/actuator/*` path returns 404. |
| 18091 | `API_PORT` | API (`app`, Spring Boot) | Direct access for Postman, `curl`, Swagger UI (`/swagger-ui.html`), Actuator, `npm run openapi:generate` and the `npm start` proxy. |
| 18092 | `GRAFANA_PORT` | Grafana | Dashboard `/d/branch-hub-observability`. Login with `GF_SECURITY_ADMIN_USER` / `GF_SECURITY_ADMIN_PASSWORD` from `.env` (anonymous Viewer on the prod overlay). |
| 18093 | `PROMETHEUS_PORT` | Prometheus | Scrapes `app:8080` on the Compose network, not the host port. |
| 18094 | `ALERTMANAGER_PORT` | Alertmanager | Silences. |
| 18095 | `POSTGRES_PORT` | PostgreSQL | For `psql`, DBeaver and `bootRun` from the host. Inside the Compose network it is `postgres:5432`. |
| none | | Kafka, Blackbox exporter | Kafka advertises `kafka:9092`, which does not resolve from the host. Only Prometheus calls the Blackbox exporter. |

18096–18099 are free and reserved for Branch Hub. The compose file holds the defaults; set a variable in `.env` only to override one.

## Database URLs

The database is `branchhub`, with user `branchhub` (password `branchhub` in `.env.example`). Which URL to use depends on where the API runs:

| API runs… | `DB_URL` |
| --------- | -------- |
| In Docker Compose (service `app`) | `jdbc:postgresql://postgres:5432/branchhub` (the default in `.env.example`) |
| On your machine (Gradle or IDE), using the Compose Postgres | `jdbc:postgresql://localhost:18095/branchhub` (Compose maps `127.0.0.1:18095` to 5432) |

**Upgrading from a database Hibernate created:** Flyway now owns the schema. A database from before that change has tables but no `flyway_schema_history`, so Flyway refuses to migrate it and the API does not start. Run `docker compose down -v` once and start again; this deletes local data only, and the super admin is seeded again at startup.

**Upgrading from the old ports:** before the 18090–18095 block, the stack published 8080, 8081, 5433, 9092, 9090, 9093, 9115 and 3000 on all interfaces. Remove any `WEB_PORT=8080` / `API_PORT=8081` lines from your `.env`, run `docker compose up -d` to recreate the containers, and update your SSH port forwards and database clients.

**Upgrading an older checkout:** before the rename to Branch Hub, the database had a different name and user. Update the `POSTGRES_*` and `DB_*` lines in your `.env` to match `.env.example`, then start from an empty volume with `docker compose down -v`. Older checkouts had no Compose project name, so Compose used the folder name; if those containers are still running, remove them first with `docker compose -p <folder-name> down -v`. This deletes local data only; the super admin is seeded again at startup.

## Default login

On startup the API creates a `SUPER_ADMIN` from `.env` if no account with that email exists yet. With the values from `.env.example`:

| Field | Value |
| ----- | ----- |
| Email | `admin@example.com` (`APP_SUPER_ADMIN_EMAIL`) |
| Password | `Admin_ChangeMe_2026!` (`APP_SUPER_ADMIN_PASSWORD`) |

If you changed `APP_SUPER_ADMIN_EMAIL` or `APP_SUPER_ADMIN_PASSWORD`, use your values. Login uses these two variables only.

## Troubleshooting

**Login returns `422 Unprocessable Content` with "Invalid credentials".** The API rejected the email or password. Check that:

- `APP_SUPER_ADMIN_PASSWORD` in `.env` has no quotes around it and no trailing spaces.
- You restarted the `app` container after changing `.env` (`docker compose up -d app`).
- The account did not already exist with an older password. The super admin is only created when no account with that email exists, so changing the password in `.env` does not update an existing account. To start over, run `docker compose down -v` (this deletes local data).

**A container stays `unhealthy`.** Check its logs with `docker compose logs <service>`. `kafka` and `blackbox` have no healthcheck, so they only show `Up`.

## Run without Docker

Day-to-day development runs `npm start` and, when needed, `./gradlew bootRun` on your machine against the stack's host ports. Run the API and the SPA on your machine, and use the Compose Postgres container as the database.

1. Start only PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

2. Start the API on port 8080 (from `backend/`):

   ```bash
   export DB_URL=jdbc:postgresql://localhost:18095/branchhub
   export DB_USERNAME=branchhub DB_PASSWORD=branchhub
   export JWT_SECRET='a-local-secret-of-at-least-32-characters'
   export APP_NOTIFICATIONS_ENABLED=false     # no Kafka: turns off the outbox relay and the consumer
   ./gradlew bootRun
   ```

   - The Compose Kafka advertises itself as `kafka:9092`, which only resolves inside the Compose network. `APP_NOTIFICATIONS_ENABLED=false` lets the API run without it; branch writes still save their outbox events.
   - Flyway creates the tables on startup, and Hibernate only validates them.
   - On the server, the stack's API already uses port 8080 inside its container only, so host port 8080 is free for `bootRun`. Use the stack's database user and password if they are not the `.env.example` defaults.

3. Start the SPA (from `frontend/`):

   ```bash
   npm ci
   npm start
   ```

   Open `http://localhost:4200`. The dev server proxies `/api` to `http://localhost:18091`, the API of the Compose stack. To use the API from step 2 instead, change `target` in `frontend/proxy.conf.json` to `http://localhost:8080` (do not commit that change).

   If the stack runs the prod overlay, its refresh cookie is `Secure`. Browsers still keep it on `http://localhost:4200` and `http://127.0.0.1:18090`, because they treat `localhost` and `127.0.0.1` as secure contexts. Any other plain-HTTP origin, such as a LAN IP, cannot keep the session.

## Run the tests

```bash
# Backend (from backend/). Every Spring context test uses a shared Testcontainers PostgreSQL, so Docker must be running.
./gradlew clean test

# Frontend unit tests and production build (from frontend/)
npm test
npm run build

# End-to-end smoke tests against the running Compose stack (from frontend/)
# E2E_BASE_URL defaults to http://localhost:18090
E2E_PASSWORD='<your APP_SUPER_ADMIN_PASSWORD>' npm run test:e2e:smoke
E2E_PASSWORD='<your APP_SUPER_ADMIN_PASSWORD>' npm run test:e2e:admin-smoke
```

The opt-in Alertmanager test sends a real alert to your Discord webhook:

```bash
RUN_ALERTMANAGER_TEST=true ./gradlew test --tests AlertmanagerDiscordIntegrationTest
```

## Server mode: the prod overlay

The server runs one stack, the published one, on the prod overlay (`docker-compose.prod.yml`). It is served at `https://code-branchhub.jotaeme.dev` (SPA, `/api`, Swagger) and `https://code-branchhub-grafana.jotaeme.dev` (Grafana) through the `server-tuf` Cloudflare Tunnel.

**Prerequisite:** the external `edge` Docker network must exist. The `jotaeme-ingress` stack creates it and runs `cloudflared` on it. On a machine without it, run `docker network create edge` before using the overlay.

On the server, `.env` holds the prod values (start from `.env.prod.example` and replace every `ChangeMe` value) and sets `COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml`. A plain `docker compose up -d --build` then always deploys the overlay, so dev settings can never replace the public demo:

```bash
cp .env.prod.example .env           # on the server; then replace every ChangeMe value
cp monitoring/alertmanager/discord-webhook-url.example monitoring/alertmanager/discord-webhook-url   # put the real webhook in it
docker compose up -d --build
docker compose config | grep SPRING_PROFILES_ACTIVE   # docker,prod
```

On another machine, run the overlay explicitly with `docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d --build`.

What the overlay changes:

- `web` and `grafana` also join the `edge` network, so `cloudflared` reaches them by container name. Every other service stays on the private Compose network.
- `app` runs the `docker,prod` profiles with a `Secure` refresh cookie and Swagger on. Actuator exposes `health,info,prometheus` and is open on the private network so Prometheus can scrape it; Nginx serves only `/actuator/health` publicly.
- Grafana allows anonymous `Viewer` access with the login form and sign-up disabled. The admin account stays configured for `grafana-cli` through `docker exec`.
- Alertmanager reads the real Discord webhook from `monitoring/alertmanager/discord-webhook-url`.

## Next steps

- [Architecture](architecture.md)
- [Backend README](../backend/README.md) and [frontend README](../frontend/README.md)
- [Security notes](security.md)
- [Postman collection](postman/branch-hub.postman_collection.json)
