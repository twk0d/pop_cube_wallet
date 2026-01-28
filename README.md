# 💳 Digital Wallet API - Modular Monolith

This is a Financial/Fintech API designed to demonstrate advanced architectural patterns using **Java 25**, **Spring Modulith**, and **GraalVM**. The project implements a digital wallet system where users can manage accounts, perform transactions, and receive notifications.

## 🎯 Project Scope

The primary goal of this project is to consolidate advanced software engineering concepts into a single, maintainable, and scalable **Modular Monolith**.

### Core Features
* **Account Management:** KYC (Know Your Customer) flow, account creation, and balance tracking.
* **Transaction Engine:** Secure peer-to-peer (P2P) transfers, deposits, and withdrawals with ACID compliance.
* **Audit Logging:** Comprehensive history of all financial movements.
* **Notifications:** Real-time alerts for successful or failed operations.

### Advanced Learning Scenarios (MVP)
* **Statement Projection (CQRS Read Side):** Denormalized statement table updated asynchronously from `TransactionCompletedEvent`.
* **Account Lock States:** Account aggregate with ACTIVE/BLOCKED/PENDING_KYC; BLOCKED prevents debits but allows credits.
* **Pockets/Sub-accounts:** Pockets/caixinhas add up to the total wallet balance; transfers cannot spend pocket funds without authorization.
* **Daily Transfer Limits:** Policy checks daily accumulated amount before allowing new debits.
* **Compensating Transactions:** Issue reversal/estorno when downstream steps fail, simulating saga behavior inside the monolith.

---

## 🏗️ Architectural Foundations

The project is built upon four main pillars documented in our [ADRs](./docs/arch-log/)

1.  **Domain-Driven Design (DDD):** Strategic mapping of the banking domain into Bounded Contexts.
2.  **Modular Monolith:** Physical and logical separation of modules using **Spring Modulith**, ensuring low coupling.
3.  **Hexagonal Architecture (Ports & Adapters):** Strict **Dependency Inversion (DIP)** where the business logic (Core) is isolated from external technologies.
4.  **CQRS:** Separation of Command (Write) and Query (Read) flows to optimize performance.

---

## 📡 Event-Driven Communication

To maintain strict isolation between modules (e.g., `Transaction` and `Notification`), we avoid direct service calls. Instead, we use **Internal Domain Events** managed by Spring Modulith.

### How it works:
* **Decoupling:** The `Transaction` module publishes `TransactionCompletedEvent` and doesn't care who consumes it.
* **Event Publication Registry:** We use Spring Modulith's **Event Publication Registry** to ensure "at-least-once" delivery. If the notification service is down, the event is persisted in the database and retried later.
* **Asynchronicity:** Notifications are processed in separate threads, ensuring the main transaction flow remains fast and responsive.

---

## 🛠️ Technology Stack

* **Runtime:** Java 25 (GraalVM)
* **Framework:** Spring Boot 4.0.2 (with Spring Modulith)
* **Architecture Documentation:** jMolecules (DDD & CQRS annotations)
* **Data Persistence:** PostgreSQL / Spring Data JPA
* **Resilience:** Resilience4j (Circuit Breaker)
* **Mapping:** MapStruct 1.6.3
* **Build Tool:** Gradle

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

Create a `.env` file in the project root (or configure via environment variables):

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pop_cube_wallet
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

## 7. Accessing the API

Once the application is running, access the Swagger UI documentation:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

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