## **CQRS Strategy for MVP**

**Date:** `2026-01-26`

**Status:** `Accepted`

**Reviewer:** `Koda Turqui`

---

### **Context**

The MVP requires a clear separation of commands and queries while keeping the system simple to operate as a modular monolith. We need to document how read models will be materialized and how transactional guarantees are kept on the write side, including idempotency for transfer commands.

---

### **Decision**

- Commands run inside a single transactional boundary per bounded context, enforcing invariants at the aggregate level (e.g., sufficient balance before debit).
- The system emits `TransactionCompletedEvent` after a successful commit; listeners update read models or trigger notifications asynchronously.
- Queries can use dedicated read models (materialized views or query-side tables) to keep response time low without impacting write transactions.
- Idempotency for P2P transfers is enforced by a client-supplied deduplication key stored with the command processing record.

---

### **Consequences**

- Write side remains strongly consistent; read side may be eventually consistent relative to the latest command.
- Separate schemas/tables for read models allow optimized indexes and denormalized projections without affecting write performance.
- Consumers of `TransactionCompletedEvent` must handle at-least-once delivery; handlers must be idempotent.
- Future microservice extraction keeps the same contract: command boundary per context and event-driven projections for queries.
