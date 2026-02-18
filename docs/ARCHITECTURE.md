# 🏗️ Architecture Guide

This document explains the architectural decisions, patterns, and structure of the Digital Wallet API. It is intended for developers joining the project or reviewing the codebase for the first time.

For the rationale behind each decision, see the [Architecture Decision Records](./docs/arch-log/).

---

## Why a Modular Monolith?

The system is built as a **Modular Monolith** ([ADR-0002](./docs/arch-log/0002-modular-monolith.md)). This means:

- **Single deployable artifact** — one Spring Boot application, one JVM process.
- **Strict module boundaries** — each module is an autonomous component with its own domain model, database schema, and public API. No cross-module database joins.
- **Designed for extraction** — modules communicate via events and thin synchronous interfaces, so any module can be extracted into a microservice with minimal refactoring.

Spring Modulith enforces these boundaries at test time via `ApplicationModules.of(Application.class).verify()`, which fails the build if a module accesses another module's internals.

---

## Bounded Contexts

The application is divided into **5 Bounded Contexts** ([ADR-0010](./docs/arch-log/0010-audit-bounded-context.md)), each mapped to a top-level Java package:

```text
com.edu.api.pop_cube_wallet
├── account/         # The Identity
├── wallet/          # The Vault
├── transaction/     # The Engine
├── audit/           # The Record
├── notification/    # The Messenger
└── shared/          # Cross-cutting (exceptions, OpenAPI config)
```

### Account — The Identity

| Aspect | Detail |
|--------|--------|
| **Responsibility** | User onboarding, CPF/email validation, account lifecycle states |
| **Aggregate Root** | `Account` |
| **Value Objects** | `Cpf` (11-digit checksum validation), `Email` (regex validation) |
| **Schema** | `account_schema` → table `accounts` |
| **Public API** | `AccountApi` — exposes `existsAndActive()`, `exists()`, `findById()` |
| **Events Published** | `AccountCreatedEvent` |

### Wallet — The Vault

| Aspect | Detail |
|--------|--------|
| **Responsibility** | Balance management via double-entry ledger, optimistic locking |
| **Aggregate Root** | `Wallet` (with `@Version` for optimistic concurrency) |
| **Entities** | `LedgerEntry` (CREDIT / DEBIT) |
| **Schema** | `wallet_schema` → tables `wallets`, `ledger_entries` |
| **Public API** | `WalletApi` — exposes `debit()`, `credit()`, `getBalance()` |
| **Events Consumed** | `AccountCreatedEvent` → auto-creates a wallet for the new account |

### Transaction — The Engine

| Aspect | Detail |
|--------|--------|
| **Responsibility** | P2P transfer orchestration, idempotency, CQRS statement projection |
| **Aggregate Root** | `Transaction` |
| **Read Model** | `StatementEntry` (CQRS projection, `@QueryModel`) |
| **Schema** | `transaction_schema` → tables `transactions`, `statement_entries` |
| **Events Published** | `TransactionCompletedEvent` |
| **Events Consumed** | `TransactionCompletedEvent` → materializes statement entries (SENT + RECEIVED) |

### Audit — The Record

| Aspect | Detail |
|--------|--------|
| **Responsibility** | Immutable audit trail of all financial movements |
| **Entity** | `AuditEntry` |
| **Schema** | `audit_schema` → table `audit_entries` |
| **Events Consumed** | `TransactionCompletedEvent` → creates P2P_TRANSFER_SENT + P2P_TRANSFER_RECEIVED entries |

### Notification — The Messenger

| Aspect | Detail |
|--------|--------|
| **Responsibility** | Reactive alerts for completed transfers (mock implementation in MVP) |
| **Schema** | None — stateless event consumer |
| **Events Consumed** | `TransactionCompletedEvent` → logs a mock notification |

---

## Hexagonal Architecture (Per Module)

Each module follows **Hexagonal Architecture (Ports & Adapters)** ([ADR-0004](./docs/arch-log/0004-internal-modules-structure.md)):

```text
┌──────────────────────────────────────────────────────┐
│                     Module                           │
│                                                      │
│  ┌─────────────┐    ┌──────────────────────────┐     │
│  │  web/       │    │  application/             │     │
│  │  Controller ├───►│  UseCase (Input Port)     │     │
│  │  (Primary   │    │  CommandHandler           │     │
│  │   Adapter)  │    │  QueryHandler             │     │
│  └─────────────┘    └─────────┬────────────────┘     │
│                               │                      │
│                               ▼                      │
│                    ┌──────────────────────┐           │
│                    │  domain/             │           │
│                    │  Aggregate Root      │           │
│                    │  Value Objects       │           │
│                    │  Repository (Port)   │           │
│                    └──────────┬───────────┘           │
│                               │                      │
│                               ▼                      │
│                    ┌──────────────────────┐           │
│                    │  infrastructure/     │           │
│                    │  PersistenceAdapter  │           │
│                    │  (Secondary Adapter) │           │
│                    │  JPA Entity          │           │
│                    │  Spring Data Repo    │           │
│                    └──────────────────────┘           │
└──────────────────────────────────────────────────────┘
```

### Layer Rules

| Layer | Can Depend On | Cannot Depend On |
|-------|---------------|------------------|
| `domain/` | Nothing (pure POJOs + jMolecules) | `application/`, `infrastructure/`, `web/`, Spring |
| `application/` | `domain/` | `infrastructure/`, `web/` |
| `web/` | `application/` (use cases only) | `domain/` internals, `infrastructure/` |
| `infrastructure/` | `domain/` (implements ports) | `web/`, `application/` internals |

### jMolecules Annotations

The codebase uses jMolecules annotations to make architectural roles explicit:

| Annotation | Layer | Example |
|------------|-------|---------|
| `@AggregateRoot` | Domain | `Account`, `Wallet`, `Transaction` |
| `@Entity` | Domain | `LedgerEntry`, `AuditEntry` |
| `@ValueObject` | Domain | `Cpf`, `Email` |
| `@Repository` | Domain (port) | `AccountRepository`, `WalletRepository` |
| `@QueryModel` | Domain (CQRS) | `StatementEntry` |
| `@Command` | Application | `CreateAccountCommand`, `TransferCommand` |
| `@PrimaryPort` | Application | `CreateAccountUseCase`, `TransferUseCase` |
| `@SecondaryPort` | Domain | `AccountRepository`, `TransactionRepository` |
| `@PrimaryAdapter` | Web | `AccountController`, `TransactionController` |
| `@SecondaryAdapter` | Infrastructure | `AccountPersistenceAdapter`, `WalletPersistenceAdapter` |

---

## CQRS Strategy

The application follows **CQRS** ([ADR-0006](./docs/arch-log/0006-cqrs-strategy.md)) with clear separation of commands and queries:

### Commands (Write Side)

| Command | Handler | Module | Side Effects |
|---------|---------|--------|--------------|
| `CreateAccountCommand` | `AccountCommandHandler` | Account | Persists account, publishes `AccountCreatedEvent` |
| `TransferCommand` | `TransactionCommandHandler` | Transaction | Validates accounts, debits/credits wallets, persists transaction, publishes `TransactionCompletedEvent` |

### Queries (Read Side)

| Query | Handler | Module | Data Source |
|-------|---------|--------|-------------|
| `GetAccountQuery` | `AccountQueryHandler` | Account | `accounts` table |
| `GetBalanceQuery` | `WalletQueryHandler` | Wallet | `wallets` table |
| `GetStatementQuery` | `StatementQueryHandler` | Transaction | `statement_entries` table (CQRS projection) |
| Audit query | `AuditQueryHandler` | Audit | `audit_entries` table |

### Statement Projection

The `statement_entries` table is a **denormalized read model** fed asynchronously by `TransactionCompletedEvent`:

```text
TransactionCommandHandler
    │
    ├── Persists Transaction (write model)
    └── Publishes TransactionCompletedEvent
                    │
                    ▼
        StatementProjectionHandler
            │
            ├── Creates SENT entry (source account)
            └── Creates RECEIVED entry (destination account)
```

The write side (`transactions` table) is strongly consistent. The read side (`statement_entries`) is **eventually consistent** — there may be a brief delay between a transfer completing and the statement reflecting it.

---

## Event-Driven Communication

Inter-module communication uses **Asynchronous Domain Events** ([ADR-0008](./docs/arch-log/0008-inter-module-communication.md)) via Spring Modulith's `ApplicationEventPublisher`.

### Event Flow Diagram

```text
┌──────────┐                        ┌──────────────┐
│ Account  │  AccountCreatedEvent   │   Wallet     │
│ Module   ├───────────────────────►│   Module     │
│          │                        │              │
│ Creates  │                        │ Auto-creates │
│ account  │                        │ wallet (₀)   │
└──────────┘                        └──────────────┘

┌──────────────┐                    ┌──────────────────┐
│ Transaction  │ TransactionCompleted│ Statement        │
│ Module       ├────────────────────►│ ProjectionHandler│
│              │        Event        │ (SENT+RECEIVED)  │
│ Orchestrates │                    └──────────────────┘
│ P2P transfer │                    ┌──────────────────┐
│              ├────────────────────►│ AuditEventHandler│
│              │                    │ (audit entries)  │
│              │                    └──────────────────┘
│              │                    ┌──────────────────┐
│              ├────────────────────►│ Notification     │
│              │                    │ Listener (log)   │
└──────────────┘                    └──────────────────┘
```

### Delivery Guarantees

- **At-least-once delivery:** Spring Modulith's **Event Publication Registry** persists events to the database before dispatching. If the application crashes after a transaction commits but before an event handler runs, the event is retried on restart.
- **Idempotent handlers:** All event consumers must handle duplicate deliveries. For example, `AuditEventHandler` checks if an audit entry for the given `transactionId` already exists before inserting.
- **Listener annotation:** All consumers use `@ApplicationModuleListener` (transactional and asynchronous by default).

### Synchronous Cross-Module Calls

Not all inter-module communication is async. Two **synchronous in-process APIs** exist for operations that require an immediate return value ([ADR-0008](./docs/arch-log/0008-inter-module-communication.md)):

| Interface | Consuming Module | Methods |
|-----------|-----------------|---------|
| `AccountApi` | Transaction | `existsAndActive(UUID)`, `exists(UUID)`, `findById(UUID)` |
| `WalletApi` | Transaction | `debit(...)`, `credit(...)`, `getBalance(UUID)` |

These are plain Java interfaces implemented by the owning module's application layer. In a future microservice extraction, they would be replaced by HTTP clients with Resilience4j patterns ([ADR-0009](./docs/arch-log/0009-resilience4j-deferred.md)).

---

## Wallet Auto-Creation Flow

When a new account is created, a wallet is automatically provisioned:

1. `AccountCommandHandler` persists the `Account` aggregate and publishes `AccountCreatedEvent`.
2. `WalletEventHandler` (in the Wallet module) listens for `AccountCreatedEvent`.
3. The handler calls `Wallet.create(accountId)` to create a zero-balance wallet.
4. The new wallet is persisted in `wallet_schema.wallets`.

This ensures every account always has exactly one wallet, without the Account module needing to know about wallets.

---

## Deadlock Prevention

The `TransactionCommandHandler` implements **deterministic lock ordering** to prevent deadlocks during concurrent opposite-direction P2P transfers (e.g., A→B and B→A simultaneously).

### The Problem

Without ordering, two concurrent transfers could each lock one wallet row first, then wait for the other — a classic **ABBA deadlock**:

```text
Transfer A→B: LOCK wallet_A, then LOCK wallet_B
Transfer B→A: LOCK wallet_B, then LOCK wallet_A
              ↑ deadlock — each holds what the other needs
```

### The Solution

Both transfers always acquire the wallet lock with the **lower UUID first**, regardless of which is source or destination:

```text
Transfer A→B: LOCK min(A,B), then LOCK max(A,B)
Transfer B→A: LOCK min(A,B), then LOCK max(A,B)
              ↑ same order — no deadlock possible
```

This is implemented by comparing `sourceAccountId` and `destinationAccountId` via `UUID.compareTo()` before deciding whether to debit-then-credit or credit-then-debit.

---

## Account State Guarding

The Transaction module enforces account state rules without tight coupling to the Account module:

| Validation | Rule | Method Used |
|------------|------|-------------|
| **Source account (debit)** | Must exist **and** be `ACTIVE` | `AccountApi.existsAndActive(UUID)` |
| **Destination account (credit)** | Must exist (any status) | `AccountApi.exists(UUID)` |

This means:
- `BLOCKED` and `PENDING_KYC` accounts **can receive** credits (money in).
- `BLOCKED` and `PENDING_KYC` accounts **cannot send** debits (money out).

On failure, the handler distinguishes "not found" (404) from "not active" (state error) by falling back to `exists()` to produce the correct error response.

---

## Database Isolation: Schema-per-Module

Each module owns a dedicated PostgreSQL schema ([ADR-0007](./docs/arch-log/0007-database-isolation.md)):

| Module | Schema | Tables |
|--------|--------|--------|
| Account | `account_schema` | `accounts` |
| Wallet | `wallet_schema` | `wallets`, `ledger_entries` |
| Transaction | `transaction_schema` | `transactions`, `statement_entries` |
| Audit | `audit_schema` | `audit_entries` |
| Notification | — | No tables (stateless) |

### Rules

- **No foreign keys** between schemas. Referential integrity across modules is maintained via domain events.
- **No cross-schema joins.** If a module needs data from another module, it calls the module's public API.
- **Flyway migrations** are stored in a single `db/migration/` folder with sequential versioning (`V1` through `V5`). All schemas are listed in `spring.flyway.schemas` so Flyway manages them together.

---

## How to Add a New Module

Follow this checklist to add a 6th bounded context (e.g., `payment-gateway`):

### 1. Create the package structure

```text
com.edu.api.pop_cube_wallet.paymentgateway/
├── package-info.java           # @ApplicationModule annotation
├── PaymentGatewayApi.java      # Public API interface (if other modules need it)
├── domain/
│   ├── Payment.java            # Aggregate root (@AggregateRoot)
│   └── PaymentRepository.java  # Output port (@Repository, @SecondaryPort)
├── application/
│   ├── CreatePaymentUseCase.java       # Input port (@PrimaryPort)
│   ├── CreatePaymentCommand.java       # Command record (@Command)
│   └── PaymentCommandHandler.java      # Implements use case
├── infrastructure/
│   └── persistence/
│       ├── PaymentJpaEntity.java       # JPA entity
│       ├── SpringDataPaymentRepo.java  # Spring Data interface
│       └── PaymentPersistenceAdapter.java  # Implements domain port (@SecondaryAdapter)
└── web/
    └── PaymentController.java          # REST controller (@PrimaryAdapter)
```

### 2. Create the database schema

Add a Flyway migration (e.g., `V6__create_payment_schema.sql`):

```sql
CREATE SCHEMA IF NOT EXISTS payment_schema;
CREATE TABLE payment_schema.payments ( ... );
```

### 3. Register the schema

Add `payment_schema` to `spring.flyway.schemas` in `application.properties`.

Add the schema to the H2 `INIT` clause in `application-test.properties`.

### 4. Update ADR-0007

Add the new schema mapping to the decision table.

### 5. Wire events (if applicable)

- To publish: inject `ApplicationEventPublisher` and call `publishEvent()`.
- To consume: create a handler method annotated with `@ApplicationModuleListener`.

### 6. Verify

Run `./gradlew test` — the `ModularityTests` class will automatically detect the new module and verify it follows the dependency rules.

---

## Related Documents

| Document | Description |
|----------|-------------|
| [README.md](./README.md) | Project overview, quick start, and feature summary |
| [API.md](./API.md) | Complete REST API reference with schemas and examples |
| [EVENTS.md](./EVENTS.md) | Domain event catalog with payloads and idempotency strategies |
| [DEVELOPMENT.md](./DEVELOPMENT.md) | Developer onboarding, local setup, and testing guide |
| [ADRs](./docs/arch-log/) | Architecture Decision Records (ADR-0001 through ADR-0010) |
