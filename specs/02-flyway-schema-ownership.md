# SPEC 02 — Make Flyway own the database schema

> **Status:** Approved
> **Depends on:** SPEC 01
> **Date:** 2026-11-05
> **Objective:** Make Flyway create the database schema on every startup, with Hibernate only validating it, and run every Spring context test against a real PostgreSQL built by those migrations.

## Why this spec exists

SPEC 01's baseline health pass found that Flyway never runs:

- Spring Boot 4 auto-configures Flyway only through `spring-boot-starter-flyway`. `backend/build.gradle` declares only `flyway-core` and `flyway-database-postgresql`.
- The `docker` profile hides the problem with `spring.jpa.hibernate.ddl-auto=update`, so Hibernate creates the tables.
- The default profile has `ddl-auto=validate`, so a run without the `docker` profile expects tables that nothing creates.
- Seven test classes start a full Spring context on embedded H2. With the starter, Flyway runs the Postgres-only scripts on H2 and about 29 tests fail.
- `BranchHubApplicationTests` excludes Boot 3 auto-configuration class names that no longer exist in Boot 4.
- The two integration tests build their tables with hand-written `CREATE TABLE` statements instead of the migrations.

Flyway has never run against any database, so no `flyway_schema_history` table and no checksum exist anywhere. There is no production or shared database. The owner decided that the existing `V1`–`V4` scripts can be rewritten freely in this spec, once.

## Scope

**In:**

- Add `spring-boot-starter-flyway` so Flyway runs on startup in every profile.
- Replace `V1`–`V4` with a single baseline script, `V1__init_schema.sql`, that matches the JPA entities exactly.
- Drop the redundant `iam_user_email_idx` from the baseline. `email UNIQUE` already creates `iam_user_email_key`, which serves the same lookups.
- Remove the `ddl-auto=update` override from the `docker` profile, so every profile uses `validate`.
- Upgrade Testcontainers from the pinned `1.21.4` to the version Spring Boot 4.0.5 manages (`2.0.4`), and wire PostgreSQL through `@ServiceConnection`.
- One shared, singleton PostgreSQL test container used by every Spring context test. Remove H2 from the build.
- Move the seven H2-based test classes and the two integration tests onto the shared container. Remove the hand-written DDL from the integration tests.
- Fix the stale Boot 3 excludes in `BranchHubApplicationTests`.
- A new `FlywayMigrationIntegrationTest` that proves the migration ran.
- Docs: remove the "Flyway does not run" known issue and document the new test setup and the one-time `docker compose down -v`.

**Out of scope (for future specs):**

- CI with GitHub Actions and a status badge (next spec, now unblocked).
- `spring.flyway.baseline-on-migrate` or any migration path for Hibernate-created databases. The owner resets the local volume instead.
- Converting the `*ControllerWebMvcTest` classes into real `@WebMvcTest` slices without a database.
- Schema changes beyond aligning the baseline with the current entities and dropping `iam_user_email_idx` (new tables, new columns, new indexes, renames).
- Seed data in migrations. `IamBootstrapRunner` keeps seeding the base roles and the SUPER_ADMIN.
- Repeatable (`R__`) migrations, Flyway callbacks, or a separate migration user.
- Upgrading any other dependency.

## Data model

This spec introduces no new tables or columns. It moves the existing schema into one baseline script.

### Migration files

| Before | After |
| ------ | ----- |
| `V1__create_branch_table.sql` | removed |
| `V2__create_outbox_and_processed_event_tables.sql` | removed |
| `V3__create_iam_tables.sql` | removed |
| `V4__create_iam_audit_log.sql` | removed |
| — | `V1__init_schema.sql` |

`V1__init_schema.sql` creates, in this order: `branch`, `outbox_event` (with `idx_outbox_event_status_created_at`), `processed_event`, `iam_role`, `iam_user`, `iam_user_role`, `iam_audit_log`, and the existing IAM indexes. Rules for the file:

- Column names, types, nullability, lengths, defaults, keys and indexes come from today's `V1`–`V4`, except `iam_user_email_idx`, which is not carried over.
- Where a JPA entity disagrees with those scripts, the entity wins, and the script is changed. `ddl-auto=validate` is the referee. Each change is listed in the PR notes.
- No `IF NOT EXISTS`. A versioned script runs exactly once.
- No "template baseline" or IAM-wave comments. One header comment says what the file is.

After this spec merges and Flyway runs, `V1__init_schema.sql` is frozen. Every later change is a new `V2__…`, `V3__…` file, as the `CLAUDE.md` rule says.

### Configuration

```properties
# application.properties (unchanged)
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# application-docker.properties
# the line spring.jpa.hibernate.ddl-auto=update is removed
```

### Gradle test dependencies

```groovy
// added
implementation 'org.springframework.boot:spring-boot-starter-flyway'
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
testImplementation 'org.testcontainers:testcontainers-postgresql'

// removed
// set('testcontainersVersion', '1.21.4')
// testImplementation platform("org.testcontainers:testcontainers-bom:…")
// testImplementation 'com.h2database:h2'
// testImplementation 'org.testcontainers:junit-jupiter'
// testImplementation 'org.testcontainers:postgresql'
```

`flyway-core` stays only if the starter does not bring it. `flyway-database-postgresql` stays.

### Shared test container

`backend/src/test/java/dev/jotaeme/branchhub/support/AbstractPostgresIntegrationTest.java`:

- An abstract class annotated `@SpringBootTest`.
- A `static final` `org.testcontainers.postgresql.PostgreSQLContainer` on `postgres:18` (the Compose image), annotated `@ServiceConnection`.
- The container is started once in a static initializer, not through `@Testcontainers` and `@Container`, so it lives for the whole Gradle test JVM. Spring caches contexts between classes, so a container stopped after one class would break the next class that reuses a cached context. Ryuk removes the container when the JVM exits.

"Shared" means one PostgreSQL container per `./gradlew test` run, which every context test class uses, instead of one database per class.

Tests that extend it:

| Class | Change |
| ----- | ------ |
| `BranchHubApplicationTests` | extends the base; the `spring.autoconfigure.exclude` property is removed |
| `AdminAuditLogControllerWebMvcTest`, `AdminRoleControllerWebMvcTest`, `AdminUserControllerWebMvcTest`, `AuthControllerWebMvcTest`, `BranchControllerWebMvcTest`, `MeControllerWebMvcTest` | extend the base instead of declaring `@SpringBootTest` themselves |
| `BranchFlowIntegrationTest`, `LoginLockoutIntegrationTest` | extend the base; their own `@Container`, the datasource entries in `@DynamicPropertySource` and the `CREATE TABLE` statements are removed; `@BeforeEach` only deletes the rows of the tables they use |
| `FlywayMigrationIntegrationTest` (new, `integration/`) | extends the base |

The test `application.properties` keeps its JWT, notification and bootstrap settings. It gets no datasource URL, because `@ServiceConnection` provides it. It does get `spring.jpa.hibernate.ddl-auto=validate`: it replaces the main `application.properties` on the test classpath, so without that line Hibernate would default to `none` against PostgreSQL and no test would validate the schema.

## Implementation plan

0. **Precondition (owner):** commit or discard the pending change in `specs/01-branch-hub-publish-readiness.md` and create the spec branch. The working tree must be clean.
1. **Baseline.** Run `./gradlew clean test` from `backend/` and record the result in the PR notes.
2. **Testcontainers upgrade.** Remove the version pin and the BOM. Switch to the `testcontainers-*` artifacts and add `spring-boot-testcontainers`. Update the imports in the two integration tests to `org.testcontainers.postgresql.PostgreSQLContainer`, and change nothing else yet. Verify: `./gradlew clean test` gives the same result as step 1.
3. **Shared container.** Add `AbstractPostgresIntegrationTest`. Move the two integration tests onto it and remove their own containers and datasource properties. Keep their hand-written DDL for now. Verify: both classes pass.
4. **Baseline migration.** Write `V1__init_schema.sql` from `V1`–`V4` and delete the four old files. Do not add the starter yet. Verify: the file applies cleanly to an empty `postgres:18` database (for example `psql -f` against a throwaway container).
5. **Flyway starter and context tests.** Add `spring-boot-starter-flyway`. Move `BranchHubApplicationTests` and the six `*ControllerWebMvcTest` classes onto the base, and remove the stale excludes. Remove the H2 dependency. Wherever `validate` reports a mismatch, change `V1__init_schema.sql` to match the entity and note it in the PR. Verify: `./gradlew clean test` passes.
6. **Remove the hand-written DDL.** Delete the `CREATE TABLE` blocks from `BranchFlowIntegrationTest` and `LoginLockoutIntegrationTest`, and keep only the row cleanup. Verify: both classes pass.
7. **Proof test.** Add `FlywayMigrationIntegrationTest`. It asserts that `flyway_schema_history` has exactly one versioned row, version `1`, with `success = true`, and that the seven app tables exist. Verify: it passes.
8. **Docker profile.** Remove `spring.jpa.hibernate.ddl-auto=update` from `application-docker.properties`. The owner runs `docker compose down -v` and then `docker compose up -d --build`. Verify: the `app` container is healthy, its log shows Flyway applying version 1, and login with the `.env` super admin works.
9. **Docs.**
   - `backend/README.md`: remove the known issue, describe the single baseline and the freeze rule, and describe the shared container under "Tests and local dev" (Docker is required for `./gradlew test`).
   - `docs/getting-started.md`: replace the Flyway note with the one-time `docker compose down -v`.
   - `CLAUDE.md`: remove the "Flyway does not run yet" known issue, and say that every context test uses the shared Testcontainers PostgreSQL.
   - `CHANGELOG.md`: add an `## Unreleased` entry and remove the old known issue line.
   - `docs/roadmap/status.md`: record SPEC 02 as the last done item and CI as next.
10. **Final pass.** Repeat `./gradlew clean test`, the Compose start from an empty volume, and `npm run test:e2e:smoke` plus `npm run test:e2e:admin-smoke` against Compose.

## Acceptance criteria

- [x] `backend/build.gradle` declares `spring-boot-starter-flyway` and `spring-boot-testcontainers`, and contains no `h2`, no `testcontainers-bom` and no `testcontainersVersion`.
- [x] `backend/src/main/resources/db/migration/` contains exactly one file, `V1__init_schema.sql`, and it contains no `IF NOT EXISTS`.
- [x] `application-docker.properties` has no `ddl-auto` line, and `grep -rn 'ddl-auto' backend/src` matches only `ddl-auto=validate`, in `src/main/resources/application.properties` and `src/test/resources/application.properties`.
- [x] `grep -rn 'CREATE TABLE' backend/src/test` returns nothing.
- [x] `grep -rn 'spring.autoconfigure.exclude' backend/src/test` returns nothing.
- [x] Every test class annotated with `@SpringBootTest`, directly or through its parent, extends `AbstractPostgresIntegrationTest`.
- [x] `./gradlew clean test` passes from `backend/` with Docker running, and the test log shows one PostgreSQL container started for the whole run.
- [x] `FlywayMigrationIntegrationTest` passes and checks version `1` with `success = true`.
- [x] After `docker compose down -v && docker compose up -d --build`, the `app` container is healthy and its log contains Flyway's line for successfully applying 1 migration.
- [x] On that stack, `psql` shows `flyway_schema_history` with one row for version `1`, and the tables `branch`, `outbox_event`, `processed_event`, `iam_role`, `iam_user`, `iam_user_role` and `iam_audit_log`.
- [x] A second `docker compose restart app` logs that the schema is up to date and applies nothing.
- [x] Login in the SPA works with the `.env` super admin, and creating a branch through the SPA succeeds.
- [x] `npm run test:e2e:smoke` and `npm run test:e2e:admin-smoke` pass against the Compose stack.
- [x] `grep -rniI 'flyway does not run\|flyway migrations do not run' --exclude-dir={node_modules,.git} .` matches only in `specs/` and in older `CHANGELOG.md` releases.
- [x] `CHANGELOG.md` has an `## Unreleased` entry for this change, and `docs/roadmap/status.md` lists SPEC 02 as the last done item.

## Decisions

- **Yes:** add `spring-boot-starter-flyway`. This is the Boot 4 way to get Flyway auto-configuration. `flyway-core` alone is not enough.
- **Yes:** collapse `V1`–`V4` into one `V1__init_schema.sql`. Flyway never ran anywhere, there is no production and there is no data, so no checksum can break. One clean baseline is easier to read than four scripts with `IF NOT EXISTS` and template comments. Git keeps the history.
- **No:** add a `V5__align_schema_with_entities.sql` on top of `V1`–`V4`. That was the first proposal. It made sense only if the old scripts had been applied somewhere, and they were not.
- **Yes:** drop `iam_user_email_idx` in the baseline (amended during implementation). It duplicates the unique index on `iam_user.email`, and removing it now costs nothing, while after the freeze it would need a `V2` script.
- **Yes:** freeze `V1__init_schema.sql` once this spec merges. From then on, the never-edit-an-applied-script rule applies again.
- **Yes:** the JPA entities win over the old scripts when they disagree. The entities are what the running code uses, and `validate` checks them.
- **Yes:** `ddl-auto=validate` in every profile. Flyway owns the schema, and Hibernate fails fast on drift.
- **No:** `none` (drift goes unnoticed) or keeping `update` (it hides drift and the missing migrations, which is how this bug survived).
- **Yes:** reset the local volume once with `docker compose down -v`. Only dev data exists, and the super admin is seeded again on startup.
- **No:** `baseline-on-migrate`. It would mark a Hibernate-made schema as migrated even if it differs from the script.
- **Yes:** Testcontainers 2.x managed by Spring Boot, with `@ServiceConnection`. The owner chose to upgrade now. Only two test files used Testcontainers, so the cost is small.
- **No:** keeping Testcontainers `1.21.4` with `@DynamicPropertySource`. It works, but it keeps a manual version pin that drifts from Boot.
- **Yes:** one singleton PostgreSQL container for every context test, and no H2. Every test then runs on the same schema the migrations build. The cost is one container start per run, and Docker becomes required for `./gradlew test`.
- **No:** disabling Flyway on H2. The tests would pass but would never touch the real schema.
- **No:** rewriting the controller tests as `@WebMvcTest` slices. It is a bigger refactor and does not fix the Flyway problem.
- **Yes:** a static initializer instead of `@Container`. With `@Container`, JUnit would stop the container after each class while Spring's cached context still points to it.
- **Yes:** remove the hand-written DDL from the integration tests. It duplicated the schema and could hide a broken migration.
- **Yes:** add `FlywayMigrationIntegrationTest` as direct proof that the migration ran.
- **Yes:** the spec is written in English, like all docs in this repo.

## Risks

| Risk | Mitigation |
| ---- | ---------- |
| `validate` finds many differences between the entities and the old scripts (for example `TIMESTAMP` vs `TIMESTAMPTZ`) | Step 5 fixes them in `V1__init_schema.sql`, one at a time, and lists them in the PR. If an entity looks wrong, stop and ask instead of changing Java code. |
| The owner's existing volume still has Hibernate-made tables, so Flyway fails on a non-empty schema | Step 8 includes `docker compose down -v`. `docs/getting-started.md` says it once. |
| Tests share one database, so data from one class leaks into another | Each integration test deletes the rows it uses in `@BeforeEach`. The bootstrap SUPER_ADMIN is idempotent. |
| Different property sets create several Spring contexts on the same database, and bootstrap runs more than once | `IamBootstrapRunner` seeds only when the account is missing, so a second run does nothing. |
| `./gradlew test` now needs Docker, and the local Docker daemon or the CI runner may not have it | Documented in `backend/README.md`. GitHub-hosted Ubuntu runners have Docker, which matters for the CI spec. |
| Testcontainers 2.x renames packages and artifacts | Step 2 does only the upgrade and checks that the result matches the baseline before anything else changes. |

## What is **not** in this spec

- CI with GitHub Actions (next spec).
- `baseline-on-migrate` or any path that keeps Hibernate-created data.
- `@WebMvcTest` slice refactors.
- New schema changes, seed data in migrations, `R__` scripts or Flyway callbacks.
- Other dependency upgrades.

Each one, if it lands, goes in its own spec.
