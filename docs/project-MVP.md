# 🎯 MVP Scope: Digital Wallet API

## MVP Phases

### **Phase 1 - Core MVP (Must Have)** ✅ PRIORITY 1
These features are essential and must be implemented for MVP release:

* **Account Module:** User onboarding, CPF validation, profile query
* **Wallet Module:** Balance query, internal credit/debit via ledger
* **Transaction Module:** Basic P2P transfer with balance validation and atomicity
* **Transaction Statement:** Historical movements list
* **Notification Module:** Transfer alerts via asynchronous events

### **Phase 2 - Advanced Features (Should Have)** ⚠️ PRIORITY 2
These features enhance robustness and should be implemented after Phase 1:

* **Account State Management:** ACTIVE, BLOCKED, PENDING_KYC states
* **Daily Transfer Limits:** Policy enforcement per account
* **Statement Projection (CQRS):** Denormalized read model fed by events
* **Audit Module:** Immutable audit trail with event sourcing
* **Idempotency Keys:** Deduplication for transfer requests

### **Phase 3 - Learning Scenarios (Could Have)** 🔄 PRIORITY 3
Advanced features for future iterations and architectural learning:

* **Pockets/Sub-accounts (Caixinhas):** Support for multiple wallets per account
* **Compensating Transactions (Estorno):** Reverse movements on downstream failures
* **Account Temporary Lock:** BLOCKED state forbidding debits

---

## Functional Requirements

### **Account Module (The Identity)**
* **User Onboarding:** Simple registration (Name, CPF, Email).
* **Domain Validation:** Business rule to prevent duplicate CPFs.
* **Profile Query:** Endpoint to retrieve account holder details.

### **Wallet Module (The Vault)**
* **Balance Query:** Endpoint to retrieve current available balance for an account/wallet.
* **Internal Credit/Debit:** Apply balance changes through the ledger (no direct balance mutation from outside).
* **Ledger Integrity:** Double-entry invariant for every movement; no orphan debits/credits.

### **Transaction Module (The Core)**
* **P2P Transfer:** Execute money movement between Account A and Account B.
    * *Validation:* Must check for sufficient balance before debiting.
    * *Atomicity:* Transactional integrity for debit and credit operations.
    * *Idempotency:* Accept a deduplication key per transfer request to avoid double posting.
    * *Event Emission:* Publish `TransactionCompletedEvent` after successful commit.
* **Transaction Statement:** List of all historical movements (Query side of CQRS).

### **Audit Module (The Record)**
* **Audit Trail:** Registro imutável de todos os movimentos financeiros relevantes.
* **Event Source:** Deve consumir eventos internos (ex.: `TransactionCompletedEvent`) para gerar entradas de auditoria.
* **Query:** Endpoint para listar o histórico de auditoria por conta e por período.
* **Consistency:** Entradas devem ser geradas apenas após commit bem‑sucedido da transação.

### **Cross-Module Learning Scenarios**
* **Consolidated Statement Projection:** Maintain a denormalized Statement table (read model) fed by `TransactionCompletedEvent` to keep queries fast and decoupled from the write side.
* **Account Temporary Lock:** Account aggregate holds states (ACTIVE, BLOCKED, PENDING_KYC); BLOCKED forbids debits while still allowing credits; Transaction module must respect Account state without tight coupling.
* **Pockets/Sub-accounts:** Wallet supports pockets/caixinhas; total balance = free balance + pockets; transfers cannot consume pocket funds without explicit authorization.
* **Daily Transfer Limits:** Enforce a policy that checks daily accumulated transfer amount before approving a new debit (e.g., BRL 5,000/day).
* **Compensating Transaction (Estorno):** On downstream failure (e.g., destination credit or critical notification), create a compensating transfer to reverse the movement, simulating a saga in the monolith.

### **Notification Module (The Side-Effect)**
* **Transfer Alert:** Mock service that logs/prints a message when a transfer is received.
    * *Constraint:* Must be triggered via asynchronous Domain Events (consume `TransactionCompletedEvent`).

---

## Technical Requirements (Project Goals)

| Feature | Implementation Detail |
| :--- | :--- |
| **Module Isolation** | No direct database joins or cross-module service injection. |
| **DIP (SOLID)** | Domain layer must be pure POJO (Except by JMolecules). |
| **CQRS** | Clear separation between Commands (Write) and Queries (Read). |
| **Read Model Projection** | Denormalized Statement table fed asynchronously from `TransactionCompletedEvent` handlers. |
| **Event-Driven** | Use Spring Modulith's Event Publication Registry for inter-module sync. |
| **Database** | PostgreSQL with schemas logically separated per module (schema-per-bounded-context). |
| **Idempotency** | Deduplication key per P2P request or optimistic versioning on aggregates. |
| **Policy Enforcement** | Daily transfer limit checked via policy that reads daily accumulated amount before debit. |
| **State Guarding** | Account state (ACTIVE/BLOCKED/PENDING_KYC) checked by Transaction module through approved query boundary. |

---

## Non-Functional Requirements

* **Segurança:** Autenticação simplificada via Header `User-ID`; prever rate limiting e autorização fina para evolução.
* **Observabilidade:** Logs estruturados e correlação por trace/span id para cada transação; trilha completa da transferência e do evento de notificação.
* **Desempenho alvo:** P99 de requisições de transferência ≤ 300ms em ambiente de referência; consultas de saldo ≤ 100ms.

## Observability & Monitoring

### Metrics (Micrometer/Prometheus)
* **Transaction Metrics:**
  - `wallet.transaction.success.count` - Total successful P2P transfers
  - `wallet.transaction.failure.count` - Total failed transfers
  - `wallet.transaction.duration` - P2P transfer latency (p50, p95, p99)
  - `wallet.transfer.amount.sum` - Total amount transferred
  
* **Account Metrics:**
  - `wallet.account.creation.count` - Total accounts created
  - `wallet.account.state.gauge` - Current account states (ACTIVE, BLOCKED, PENDING_KYC)
  
* **Wallet Metrics:**
  - `wallet.balance.query.duration` - Balance query latency
  - `wallet.ledger.entries.count` - Total ledger entries

### Health Checks
* **`/actuator/health`** - Overall application health
* **`/actuator/health/db`** - PostgreSQL connection status
* **`/actuator/health/modulith`** - Spring Modulith module dependencies verification
* **`/actuator/health/readiness`** - Application readiness (all modules loaded)

### Structured Logging
* **Format:** JSON (Logstash-compatible)
* **Required Fields per Log Entry:**
  - `timestamp` - ISO 8601 format
  - `level` - INFO, WARN, ERROR, DEBUG
  - `traceId` - Distributed trace identifier
  - `spanId` - Distributed span identifier
  - `userId` - Account/User ID performing the operation
  - `operation` - Operation name (e.g., "P2P_TRANSFER", "ACCOUNT_CREATION")
  - `module` - Module name (account, transaction, notification, etc.)
  - `status` - Operation result (SUCCESS, FAILURE, PENDING)
  - `duration_ms` - Operation execution time in milliseconds
  - `message` - Human-readable log message

* **Example Log Entry (JSON):**
```json
{
  "timestamp": "2026-02-02T10:30:45.123Z",
  "level": "INFO",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "spanId": "f5d3c5a1b9e2",
  "userId": "account-uuid-123",
  "operation": "P2P_TRANSFER",
  "module": "transaction",
  "status": "SUCCESS",
  "duration_ms": 245,
  "sourceAccountId": "account-uuid-123",
  "destinationAccountId": "account-uuid-456",
  "amount": 100.00,
  "message": "P2P transfer completed successfully"
}
```

* **Log Levels Strategy:**
  - **DEBUG:** Detailed operation steps, variable states, flow control
  - **INFO:** Operation completion, state transitions, business events
  - **WARN:** Retries, fallbacks, degraded operation modes
  - **ERROR:** Transaction failures, data inconsistencies, module health issues

### Observability Stack
* **Metrics:** Micrometer + Spring Boot Actuator
* **Logging:** Logback + Logstash layout (JSON)
* **Tracing:** Spring Cloud Sleuth (for traceId/spanId propagation)
* **Monitoring:** Prometheus (metrics scraping) + Grafana (dashboards)
* **Log Aggregation:** ELK Stack or Loki (for production)

### Observability in Definition of Done
* All async operations (notifications, event handlers) must propagate traceId/spanId
* Critical paths (P2P transfer, account creation) must emit timing metrics
* All module boundaries must have corresponding health checks
* Error scenarios must include context-rich error logs with traceId for correlation

## Technical Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Event Publication Registry failure** | Lost notifications; eventual consistency broken | Implement retry mechanism with exponential backoff; maintain Dead Letter Queue for failed events; add monitoring/alerting on publication failures |
| **Race condition on concurrent transfers** | Balance inconsistency; double spending | Use Optimistic Locking on Wallet aggregate with `@Version`; implement transaction isolation level REPEATABLE_READ; add idempotency key deduplication |
| **Database schema per module complexity** | Migration difficulties; version conflicts | Use Flyway with module-specific migration paths (e.g., `classpath:db/migration/account/`, `classpath:db/migration/transaction/`); enforce naming conventions |
| **GraalVM native image issues** | Reflection errors at runtime; serialization failures | Document required reflection configs in `reflection-config.json`; test native compilation in CI/CD pipeline; use GraalVM agent during development |
| **Module dependency cycles** | Architectural violation; tight coupling | Run `ApplicationModules.of(Application.class).verify()` in every build; enforce DIP strictly; use domain events for cross-module communication |
| **Event handler ordering dependency** | Inconsistent read models; projection lag | Implement idempotent event handlers; use event versioning; store event sequence number in projections; implement eventual consistency guarantees |
| **Concurrent pocket/sub-account access** | Pocket balance mismatch | Lock wallet during pocket operations; implement pessimistic locking on pocket transfers; test high-contention scenarios |
| **Large transaction volumes overwhelming ledger** | Query performance degradation | Implement ledger partitioning by date; create indexes on `accountId`, `timestamp`; use read replicas for statement queries; consider archival strategy |
| **Spring Modulith test verification complexity** | False positives; missed violations | Document module boundaries clearly; use automated diagram generation; run modular tests in isolation before full integration |
| **Logstash JSON serialization overhead** | Increased latency on high-throughput ops | Use async logging with appender buffer; benchmark JSON serialization cost; consider sampling strategy for DEBUG logs in production |

## Technical Debt & Future Considerations

* **Payment Gateway Integration:** Currently mocked; real integration requires PCI compliance and additional security measures
* **Distributed Tracing:** Spring Cloud Sleuth suitable for monolith; review when transitioning to microservices
* **Event Store:** Consider event sourcing library (Axon Framework) for more advanced saga patterns
* **Cache Layer:** Add Redis for balance caching and reduce database load on high-traffic scenarios
* **Rate Limiting:** Implement via API Gateway or Spring Cloud Gateway; define per-user and per-endpoint limits

## Out of Scope
* Real Payment Gateway integration.
* Frontend/UI (API only).
* OAuth2/Complex Authentication (simplified via Header User-ID).

---

## Definition of Done (DoD)
1.  Architecture passes `ApplicationModules.of(Application.class).verify()`.
2.  Successful P2P transfer updates balances and triggers a notification.
3.  Swagger/OpenAPI documentation is generated and functional.
4.  Statement projection is updated asynchronously after transfers and remains eventually consistent.
5.  BLOCKED accounts reject debits but accept credits; daily transfer limit policy enforced.
6.  Pockets/caixinhas are respected in balance calculations and protected from unauthorized debits.
7.  Compensating transaction flow issues an estorno when downstream steps fail.
8.  All technical risks have documented mitigation strategies and are monitored.
9.  Event publication, retry mechanisms, and DLQ are tested and working.
10. Concurrent transfer scenarios pass load tests without balance inconsistencies.