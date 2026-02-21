# 💳 Digital Wallet API - Modular Monolith

This is a Financial/Fintech API designed to demonstrate advanced architectural patterns using **Java 25**, **Spring Modulith**, and **GraalVM**. The project implements a digital wallet system where users can manage accounts, perform transactions, and receive notifications.

## 🎯 Project Scope

The primary goal of this project is to consolidate advanced software engineering concepts into a single, maintainable, and scalable **Modular Monolith**.

### Core Features (Phase 1 — Implemented)
* **Account Management:** KYC (Know Your Customer) flow, account creation, and profile query.
* **Transaction Engine:** Secure peer-to-peer (P2P) transfers with ACID compliance, idempotency keys, and CQRS statement projection.
* **Audit Logging:** Immutable, event-driven audit trail — captures `TransactionCompletedEvent` entries with query by account and time range.
* **Notifications:** Real-time mock alerts for successful transfer operations.
* **Wallet & Ledger:** Balance management via double-entry ledger. Wallets are auto-created on account registration.

### Advanced Learning Scenarios
* **Statement Projection (CQRS Read Side):** *(Phase 1 — Implemented)* Denormalized statement table updated asynchronously from `TransactionCompletedEvent`.
* **Account Lock States:** *(Phase 1 — Partially Implemented)* Account aggregate with ACTIVE/BLOCKED/PENDING_KYC; BLOCKED prevents debits but allows credits. State transition API not yet implemented.
* **Pockets/Sub-accounts:** *(Phase 3 — Not Yet Implemented)* Pockets/caixinhas add up to the total wallet balance; transfers cannot spend pocket funds without authorization.
* **Daily Transfer Limits:** *(Phase 2 — Not Yet Implemented)* Policy checks daily accumulated amount before allowing new debits.
* **Compensating Transactions:** *(Phase 3 — Not Yet Implemented)* Issue reversal/estorno when downstream steps fail, simulating saga behavior inside the monolith.

---

## 🏗️ Architectural Foundations

The project is built upon four main pillars documented in our [ADRs](./docs/arch-log/)

1.  **Domain-Driven Design (DDD):** Strategic mapping of the banking domain into Bounded Contexts.
2.  **Modular Monolith:** Physical and logical separation of modules using **Spring Modulith**, ensuring low coupling.
3.  **Hexagonal Architecture (Ports & Adapters):** Strict **Dependency Inversion (DIP)** where the business logic (Core) is isolated from external technologies.
4.  **CQRS:** Separation of Command (Write) and Query (Read) flows to optimize performance.

For deeper technical detail, see [ARCHITECTURE.md](./ARCHITECTURE.md).

### Architecture Overview

The application is divided into **5 Bounded Contexts** ([ADR-0010](./docs/arch-log/0010-audit-bounded-context.md)):

| Module | Role | Schema | Key Aggregate |
|--------|------|--------|----------------|
| **Account** | The Identity — user profiles, KYC, account states | `account_schema` | `Account` |
| **Wallet** | The Vault — balances, ledger entries, optimistic locking | `wallet_schema` | `Wallet` |
| **Transaction** | The Engine — P2P transfers, idempotency, statement projection | `transaction_schema` | `Transaction` |
| **Audit** | The Record — immutable audit trail from domain events | `audit_schema` | `AuditEntry` |
| **Notification** | The Messenger — reactive alerts (stateless, no schema) | — | — |

---

## 📡 Event-Driven Communication

To maintain strict isolation between modules (e.g., `Transaction` and `Notification`), we avoid direct service calls. Instead, we use **Internal Domain Events** managed by Spring Modulith.

### How it works:
* **Decoupling:** The `Transaction` module publishes `TransactionCompletedEvent` and doesn't care who consumes it.
* **Event Publication Registry:** We use Spring Modulith's **Event Publication Registry** to ensure "at-least-once" delivery. If the notification service is down, the event is persisted in the database and retried later.
* **Asynchronicity:** Notifications are processed in separate threads, ensuring the main transaction flow remains fast and responsive.

### Event Flow

```text
Account Created                          P2P Transfer Completed
      │                                          │
      ▼                                          ▼
AccountCreatedEvent                  TransactionCompletedEvent
      │                                    ┌─────┼─────────┐
      ▼                                    ▼     ▼         ▼
WalletEventHandler              Statement  Audit   Notification
(auto-creates wallet)           Projection Handler  Listener
```

For the full event catalog, see [EVENTS.md](./EVENTS.md).

---

## 🛠️ Technology Stack

* **Runtime:** Java 25 (GraalVM)
* **Framework:** Spring Boot 4.0.2 (with Spring Modulith)
* **Architecture Documentation:** jMolecules (DDD & CQRS annotations)
* **Data Persistence:** PostgreSQL / Spring Data JPA / Flyway migrations
* **API Documentation:** SpringDoc OpenAPI (Swagger UI)
* **Observability:** Micrometer + Prometheus
* **Build Tool:** Gradle 9.2.1
* **Testing:** JUnit 5 / ArchUnit / Spring Modulith Test / Testcontainers / H2

---

## 📡 API Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/accounts` | Create a new account | None |
| `GET` | `/api/accounts/{accountId}` | Get account by ID | None |
| `GET` | `/api/wallets/balance` | Get wallet balance | `User-ID` header |
| `POST` | `/api/transactions/transfer` | Execute P2P transfer | `User-ID` header |
| `GET` | `/api/transactions/statement` | Get account statement (CQRS read model) | `User-ID` header |
| `GET` | `/api/audit?accountId=&from=&to=` | Query audit trail by account and time range | None |

For full request/response schemas, validations, and curl examples, see [API.md](./API.md).

---

## 📊 Observability & Metrics

All metrics are exposed via the Prometheus endpoint at `/actuator/prometheus`.

### Transaction Metrics
| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `wallet.transaction.success.count` | Counter | `type=P2P` | Successful P2P transfers |
| `wallet.transaction.failure.count` | Counter | `reason=insufficient_funds\|duplicate` | Failed transfer attempts |
| `wallet.transaction.duration` | Timer | — | P2P transfer latency (p50, p95, p99) |
| `wallet.transfer.amount.sum` | DistributionSummary | `baseUnit=BRL` | Transfer amount distribution |

### Account Metrics
| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `wallet.account.created.count` | Counter | — | Total accounts created |
| `wallet.account.state.gauge` | Gauge | `state=ACTIVE\|BLOCKED\|PENDING_KYC` | Current account count per state |

### Wallet Metrics
| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `wallet.balance.query.duration` | Timer | — | Balance query latency |
| `wallet.ledger.entries.count` | Counter | `type=CREDIT\|DEBIT` | Total ledger entries created |

### Health Endpoints
* `/actuator/health` — overall application health
* `/actuator/metrics` — Micrometer metrics index
* `/actuator/prometheus` — Prometheus scrape endpoint

---

## 📂 Project Structure

Each module follows the Hexagonal internal structure:

```text
src/main/java/com/edu/api/pop_cube_wallet
├── [module-name]/           # e.g., account, transaction
│   ├── domain/              # Entities, Aggregates, and Repository Interfaces (Ports)
│   ├── application/         # CQRS Handlers (Commands/Queries) and Use Cases
│   ├── infrastructure/      # JPA Adapters, External API Clients, Configurations
│   └── web/                 # REST Controllers (Input Adapters)
└── Application.java
```

## 🧪 Testing and Validation

We use **ArchUnit** and **Spring Modulith Test** to ensure architectural integrity:

* **Module Interaction:** Verified via `ApplicationModules.of(Application.class).verify()`.
* **DIP Check:** Domain layer is strictly forbidden from importing Infrastructure classes.
* **Documentation:** Automatic generation of module dependency diagrams and Canvas.

---

## 🚀 Getting Started

1.  **Prerequisites:** JDK 25 and Docker (for PostgreSQL).
2.  **Clone the repo:** `git clone <repository-url>`
3.  **Build the project:** `./gradlew build`
4.  **Run tests:** `./gradlew test`

## 5. Running PostgreSQL

The project uses PostgreSQL. Start it with Docker Compose:

```bash
docker-compose up -d postgres
```

This will start PostgreSQL on `localhost:5432` with default credentials configured in `.env`.

## 6. Environment Configuration

Copy the example file and adjust values as needed:

```bash
cp .env.example .env
```

The following variables are used (defaults shown):

| Variable | Default | Description |
|------------|-------------------|-------------------------------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `pop_cube_wallet` | Database name |
| `DB_USER` | `wallet_user` | Database user |
| `DB_PASS` | `wallet_pass` | Database password |
| `SERVER_PORT` | `8080` | Application HTTP port |

## 7. Accessing the API

Once the application is running, access the interactive API documentation:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

## 8. API Usage Examples

### Register a new account
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "cpf": "12345678900",
    "email": "joao@example.com"
  }'
```

### Query account balance
```bash
curl -X GET http://localhost:8080/api/wallets/balance \
  -H "User-ID: account-uuid-here"
```

### Execute P2P transfer
```bash
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -H "User-ID: sender-uuid" \
  -d '{
    "deduplicationKey": "unique-key-123",
    "destinationAccountId": "recipient-uuid",
    "amount": 100.00
  }'
```

### Query account statement
```bash
curl -X GET http://localhost:8080/api/transactions/statement \
  -H "User-ID: account-uuid-here"
```

### Query audit trail
```bash
curl -X GET "http://localhost:8080/api/audit?accountId=account-uuid&from=2026-01-01T00:00:00&to=2026-12-31T23:59:59"
```

---

## 📚 Further Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Modular monolith structure, hexagonal layers, CQRS, event flows |
| [API.md](./API.md) | Complete API reference with request/response schemas and error codes |
| [DEVELOPMENT.md](./DEVELOPMENT.md) | Developer onboarding, local setup, testing, and troubleshooting |
| [EVENTS.md](./EVENTS.md) | Domain event catalog with payloads, consumers, and idempotency |
| [ADRs](./docs/arch-log/) | Architecture Decision Records (ADR-0001 through ADR-0010) |
| [MVP Scope](./docs/project-MVP.md) | Full MVP specification and Definition of Done |