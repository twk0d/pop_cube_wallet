# 🛠️ Developer Guide

This guide walks you through setting up, running, and testing the Digital Wallet API on your local machine.

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| **JDK** | 25 (GraalVM recommended) | Runtime and compilation |
| **Docker** | 20+ | PostgreSQL container |
| **Gradle** | 9.2.1 (bundled via wrapper) | Build tool — use `./gradlew` or `gradlew.bat`, no global install needed |
| **Git** | 2.x | Source control |

> **Windows users:** Use `gradlew.bat` instead of `./gradlew` in all commands below.

---

## Local Setup

### 1. Clone the repository

```bash
git clone <repository-url>
cd pop_cube_wallet
```

### 2. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env` if your PostgreSQL setup differs from the defaults:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `pop_cube_wallet` | Database name |
| `DB_USER` | `wallet_user` | Database user |
| `DB_PASS` | `wallet_pass` | Database password |
| `SERVER_PORT` | `8080` | Application HTTP port |

### 3. Start PostgreSQL

```bash
docker run -d \
  --name pop_cube_postgres \
  -e POSTGRES_DB=pop_cube_wallet \
  -e POSTGRES_USER=wallet_user \
  -e POSTGRES_PASSWORD=wallet_pass \
  -p 5432:5432 \
  postgres:17
```

Or if the project has a `docker-compose.yml`:

```bash
docker-compose up -d postgres
```

### 4. Build the project

```bash
./gradlew build
```

This will compile, run all tests (unit + modularity), and produce the application JAR.

### 5. Run the application

```bash
./gradlew bootRun
```

The application starts on `http://localhost:8080` (or the port set in `SERVER_PORT`).

On first startup, Flyway automatically runs migrations V1–V5, creating all four schemas and their tables.

### 6. Verify it's running

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

---

## Accessing the API

### Swagger UI

Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) in your browser for the interactive API explorer.

### OpenAPI Spec

The raw OpenAPI JSON is available at [http://localhost:8080/api-docs](http://localhost:8080/api-docs).

### Quick Smoke Test

```bash
# 1. Create an account
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","cpf":"12345678909","email":"test@example.com"}' | jq .

# 2. Check balance (use the returned ID)
curl -s http://localhost:8080/api/wallets/balance \
  -H "User-ID: <account-id-from-step-1>" | jq .
```

For the full API reference, see [API.md](./API.md).

---

## Running Tests

### All tests

```bash
./gradlew test
```

This runs all test types in a single pass.

### Test types in the project

| Test | Class | What it verifies |
|------|-------|-----------------|
| **Modularity** | `ModularityTests` | Spring Modulith module boundaries — no illegal cross-module access |
| **Application Context** | `PopCubeWalletApplicationTests` | Spring context loads successfully with all beans |
| **Flyway Integration** | `FlywayMigrationIntegrationTest` | All migrations run successfully against real PostgreSQL (via Testcontainers) |

### Test configuration

- **Unit/context tests** use **H2 in-memory** database with `application-test.properties`:
  - `ddl-auto=create-drop` (JPA auto-generates schema)
  - Flyway disabled
  - All four schemas created via H2 `INIT` SQL

- **Integration tests** (Testcontainers) use a **real PostgreSQL 17** container:
  - Flyway enabled — runs actual migrations
  - Container is auto-started and stopped by JUnit

### Running a specific test

```bash
# Run only modularity tests
./gradlew test --tests "*ModularityTests"

# Run only Flyway integration test
./gradlew test --tests "*FlywayMigrationIntegrationTest"
```

---

## Viewing Metrics

### Prometheus endpoint

All Micrometer metrics are exposed at:

```
http://localhost:8080/actuator/prometheus
```

### Metrics index

Browse available metric names at:

```
http://localhost:8080/actuator/metrics
```

### Query a specific metric

```bash
# Transaction success count
curl http://localhost:8080/actuator/metrics/wallet.transaction.success.count

# Account state gauge
curl http://localhost:8080/actuator/metrics/wallet.account.state.gauge

# Transfer amount distribution
curl http://localhost:8080/actuator/metrics/wallet.transfer.amount.sum
```

For the full list of 9 custom metrics, see the [Metrics section in README.md](./README.md#-observability--metrics).

---

## Adding a New Bounded Context

Follow this checklist when creating a new module (e.g., `paymentgateway`).

### Step 1 — Create package structure

```text
com.edu.api.pop_cube_wallet.paymentgateway/
├── package-info.java              # @ApplicationModule
├── PaymentGatewayApi.java         # Public API interface (optional)
├── domain/
│   ├── Payment.java               # @AggregateRoot
│   └── PaymentRepository.java     # @Repository, @SecondaryPort
├── application/
│   ├── CreatePaymentUseCase.java  # @PrimaryPort
│   ├── CreatePaymentCommand.java  # @Command
│   └── PaymentCommandHandler.java # Implements use case
├── infrastructure/
│   └── persistence/
│       ├── PaymentJpaEntity.java
│       ├── SpringDataPaymentRepo.java
│       └── PaymentPersistenceAdapter.java  # @SecondaryAdapter
└── web/
    └── PaymentController.java     # @PrimaryAdapter
```

### Step 2 — Create `package-info.java`

```java
/**
 * Payment Gateway module — handles external payment processing.
 */
@org.springframework.modulith.ApplicationModule
package com.edu.api.pop_cube_wallet.paymentgateway;
```

### Step 3 — Create Flyway migration

Add `src/main/resources/db/migration/V6__create_payment_schema.sql`:

```sql
CREATE SCHEMA IF NOT EXISTS payment_schema;

CREATE TABLE payment_schema.payments (
    id UUID PRIMARY KEY,
    -- your columns here
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

### Step 4 — Register the schema

In `application.properties`, add `payment_schema` to:

```properties
spring.flyway.schemas=account_schema,wallet_schema,transaction_schema,audit_schema,payment_schema
```

In `application-test.properties`, add to the H2 `INIT` clause:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS ... \\;CREATE SCHEMA IF NOT EXISTS payment_schema
```

### Step 5 — Wire domain events (if applicable)

- **To publish:** inject `ApplicationEventPublisher`, call `publishEvent()`.
- **To consume:** annotate a handler method with `@ApplicationModuleListener`.
- Place event records in the module's root package (e.g., `PaymentProcessedEvent`).

### Step 6 — Update documentation

- Add the new module to [ADR-0007](./docs/arch-log/0007-database-isolation.md) (schema mapping).
- Add events to [EVENTS.md](./EVENTS.md) if applicable.

### Step 7 — Verify

```bash
./gradlew test
```

`ModularityTests` will automatically discover the new module and verify it follows all dependency rules.

---

## Common Errors and Fixes

### Database connection refused

```
Connection to localhost:5432 refused
```

**Cause:** PostgreSQL is not running.
**Fix:** Start the container:

```bash
docker start pop_cube_postgres
```

Or check if the port is different from what's in your `.env` file.

### Port already in use

```
Web server failed to start. Port 8080 was already in use.
```

**Fix:** Either stop the other process or change `SERVER_PORT` in your `.env`:

```bash
SERVER_PORT=8081
```

### Flyway migration checksum mismatch

```
FlywayException: Validate failed: Migration checksum mismatch
```

**Cause:** A migration file was modified after it was already applied.
**Fix:** Never edit applied migrations. Either:
1. Drop and recreate the database: `docker rm -f pop_cube_postgres` and re-run setup.
2. Create a new migration (e.g., `V6__fix_something.sql`) with the corrective DDL.

### H2 schema not found in tests

```
Schema "ACCOUNT_SCHEMA" not found
```

**Cause:** The `INIT` clause in `application-test.properties` is missing the schema.
**Fix:** Ensure all schemas are listed in the `INIT` SQL:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS account_schema\\;CREATE SCHEMA IF NOT EXISTS wallet_schema\\;...
```

### Module boundary violation

```
Module 'transaction' depends on non-exposed type ... of module 'account'
```

**Cause:** A class in the Transaction module is importing a non-public class from Account's internal packages.
**Fix:** Only use types from the module's root package (e.g., `AccountApi`, `AccountInfo`, `AccountCreatedEvent`). Internal packages (`domain/`, `application/`, `infrastructure/`, `web/`) are not accessible from other modules.

### Optimistic locking exception

```
ObjectOptimisticLockingFailureException: Row was updated ... by another transaction
```

**Cause:** Two concurrent requests tried to modify the same wallet simultaneously.
**Fix:** This is expected behavior — the failed request should be retried by the client with the same idempotency key. The `@Version` field on `Wallet` prevents lost updates.

---

## Project Layout Reference

```text
pop_cube_wallet/
├── build.gradle                    # Dependencies, plugins, Java 25 toolchain
├── settings.gradle                 # Project name
├── gradlew / gradlew.bat          # Gradle wrapper scripts
├── .env.example                    # Environment variable template
├── README.md                       # Project overview
├── ARCHITECTURE.md                 # Architecture deep-dive
├── API.md                          # API reference
├── DEVELOPMENT.md                  # This file
├── EVENTS.md                       # Domain event catalog
├── TODO.md                         # Backlog
├── docs/
│   ├── project-MVP.md              # MVP specification
│   └── arch-log/                   # Architecture Decision Records
│       ├── 0001-record-arch-decisions.md
│       ├── ...
│       └── 0010-audit-bounded-context.md
├── src/main/java/.../
│   ├── account/                    # Account bounded context
│   ├── wallet/                     # Wallet bounded context
│   ├── transaction/                # Transaction bounded context
│   ├── audit/                      # Audit bounded context
│   ├── notification/               # Notification bounded context
│   ├── shared/                     # Cross-cutting concerns
│   └── PopCubeWalletApplication.java
├── src/main/resources/
│   ├── application.properties      # Production config
│   └── db/migration/               # Flyway migrations (V1–V5)
└── src/test/
    ├── java/.../                    # Test classes
    └── resources/
        └── application-test.properties  # H2 test config
```

---

## Related Documents

| Document | Description |
|----------|-------------|
| [README.md](./README.md) | Project overview, features, quick start |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Modular monolith structure, hexagonal layers, event flows |
| [API.md](./API.md) | Complete REST API reference with schemas and curl examples |
| [EVENTS.md](./EVENTS.md) | Domain event catalog with payloads and idempotency strategies |
| [ADRs](./docs/arch-log/) | Architecture Decision Records (ADR-0001 through ADR-0010) |
