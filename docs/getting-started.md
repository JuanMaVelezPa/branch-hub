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

3. Open the SPA at `http://localhost:8080` and log in (see [Default login](#default-login)).

Useful commands:

```bash
docker compose logs -f web app    # follow the SPA and API logs
docker compose down               # stop the stack, keep the data
docker compose down -v            # stop the stack and delete the database volume
```

The Compose project is named `branch-hub`, so containers are `branch-hub-*` and the database volume is `branch-hub_postgres-data`.

## Ports and URLs

| Service | Host URL | Notes |
| ------- | -------- | ----- |
| SPA (`web`, Nginx) | `http://localhost:${WEB_PORT}` (default 8080) | Browser entry point. Nginx proxies `/api`, `/actuator`, `/swagger-ui` and `/v3/*` to `app`, so the browser stays same-origin and needs no CORS setup. |
| API (`app`, Spring Boot) | `http://localhost:${API_PORT}` (default 8081) | Direct access for Postman, `curl`, Swagger UI (`/swagger-ui.html`) and Actuator. |
| PostgreSQL | `localhost:5433` | For local tools such as `psql`. Inside the Compose network it is `postgres:5432`. |
| Grafana | `http://localhost:3000` | Dashboard `/d/branch-hub-observability`. Login with `GF_SECURITY_ADMIN_USER` / `GF_SECURITY_ADMIN_PASSWORD` from `.env`. |
| Prometheus | `http://localhost:9090` | Scrapes `app:8080` on the Compose network, not the host port. |
| Alertmanager | `http://localhost:9093` | |

`WEB_PORT` and `API_PORT` in `.env` change the host ports of the SPA and the API. Use them if 8080 or 8081 is already taken on your machine.

## Database URLs

The database is `branchhub`, with user `branchhub` (password `branchhub` in `.env.example`). Which URL to use depends on where the API runs:

| API runs… | `DB_URL` |
| --------- | -------- |
| In Docker Compose (service `app`) | `jdbc:postgresql://postgres:5432/branchhub` (the default in `.env.example`) |
| On your machine (Gradle or IDE), using the Compose Postgres | `jdbc:postgresql://localhost:5433/branchhub` (Compose maps host port 5433 to 5432) |

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

Run the API and the SPA on your machine, and use the Compose Postgres container as the database.

1. Start only PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

2. Start the API on port 8080 (from `backend/`):

   ```bash
   export DB_URL=jdbc:postgresql://localhost:5433/branchhub
   export DB_USERNAME=branchhub DB_PASSWORD=branchhub
   export JWT_SECRET='a-local-secret-of-at-least-32-characters'
   export APP_NOTIFICATIONS_ENABLED=false     # no Kafka: turns off the outbox relay and the consumer
   export SPRING_PROFILES_ACTIVE=docker       # lets Hibernate create the tables (see note)
   ./gradlew bootRun
   ```

   - The Compose Kafka advertises itself as `kafka:9092`, which only resolves inside the Compose network. `APP_NOTIFICATIONS_ENABLED=false` lets the API run without it; branch writes still save their outbox events.
   - Note: Flyway migrations do not run yet (a known issue, planned as the next fix). Until then the tables come from Hibernate (`ddl-auto=update` in the `docker` profile). With the default profile, the API expects the tables to exist already.

3. Start the SPA (from `frontend/`):

   ```bash
   npm ci
   npm start
   ```

   Open `http://localhost:4200`. The dev server proxies `/api` to `http://localhost:8080`, the API from step 2.

## Run the tests

```bash
# Backend (from backend/). Integration tests use Testcontainers, so Docker must be running.
./gradlew clean test

# Frontend unit tests and production build (from frontend/)
npm test
npm run build

# End-to-end smoke tests against the running Compose stack (from frontend/)
E2E_BASE_URL=http://localhost:8080 E2E_PASSWORD='<your APP_SUPER_ADMIN_PASSWORD>' npm run test:e2e:smoke
E2E_BASE_URL=http://localhost:8080 E2E_PASSWORD='<your APP_SUPER_ADMIN_PASSWORD>' npm run test:e2e:admin-smoke
```

The opt-in Alertmanager test sends a real alert to your Discord webhook:

```bash
RUN_ALERTMANAGER_TEST=true ./gradlew test --tests AlertmanagerDiscordIntegrationTest
```

## Production-like stack

```bash
cp .env.prod.example .env.prod      # then replace every ChangeMe value
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

The `prod` profile turns off Swagger, reduces the Actuator endpoints and hides health details.

## Next steps

- [Architecture](architecture.md)
- [Backend README](../backend/README.md) and [frontend README](../frontend/README.md)
- [Security notes](security.md)
- [Postman collection](postman/branch-hub.postman_collection.json)
