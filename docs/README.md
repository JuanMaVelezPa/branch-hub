# Documentation

Index of the Branch Hub documentation. Implementation details (pagination, loading states, i18n, and so on) live in the module READMEs; the roadmap files summarize phases and status.

## Start here

| Document | Contents |
| -------- | -------- |
| [Getting started](getting-started.md) | Run with Docker Compose or locally, ports, database URLs, default login, troubleshooting, tests |
| [Architecture](architecture.md) | System view, backend layers, transactional outbox, pagination, errors, auth |
| [Security](security.md) | Backend vs frontend security by phase, production builds, hardening checklist |

## Modules

| Module | Contents |
| ------ | -------- |
| [Backend](../backend/README.md) | Spring Boot API: Flyway, security matrix, pagination, metrics, alerting, tests |
| [Frontend](../frontend/README.md) | Angular SPA: stack, dev server and proxy, Docker image, i18n, OpenAPI typings, tests |

## Roadmap

| Document | Contents |
| -------- | -------- |
| [Delivery status](roadmap/status.md) | What is done and what is next: **v1.0** baseline (**B1, B2, F1, F2, H1**), **F3**, **v1.1 (IAM1)**, **v1.2 (IAM2)**, open gaps and verification commands |
| [Evolution (waves and v1.x)](roadmap/evolution.md) | The ordered plan: backend and frontend by wave, and the `v1.x` release numbers |
| [Overview and standards](roadmap/overview.md) | Principles, monorepo layout, delivery cadence, decided standards, git workflow |
| [Backend phases](roadmap/backend.md) | Phase 1 (API maturity) and Phase 2 (security and reliability) |
| [Frontend phase](roadmap/frontend.md) | Angular stack, slices, folder rules, definition of done, **F3** client query cache |
| [Identity and access (IAM1–IAM6)](roadmap/auth-platform.md) | Identity model, priorities, operations, email, OAuth and compliance, phases IAM1–IAM6 |

## API contract

- **Postman:** import [`postman/branch-hub.postman_collection.json`](postman/branch-hub.postman_collection.json). It lives with the docs so the backend and the frontend share one contract reference.
- **OpenAPI:** Swagger UI at `/swagger-ui.html` on the API and through `web` (off in the `prod` profile, on in the prod overlay, published at `https://code-branchhub.jotaeme.dev/swagger-ui.html`). The SPA generates its typings from `/v3/api-docs` (see the [frontend README](../frontend/README.md#openapi-generation-typed-contract)).

## Release notes

[CHANGELOG.md](../CHANGELOG.md) lists the `v1.x` releases.

## Language

All code, commits and documentation in this repository are written in English.
