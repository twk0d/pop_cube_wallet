# 🎯 MVP Scope: Digital Wallet API

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

## Technical Requirements (Learning Goals)

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