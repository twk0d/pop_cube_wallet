## **Strategic Design Update: Audit as Fifth Bounded Context**

**Date:** `2026-02-20`

**Status:** `Accepted`

**Reviewer:** `Koda Turqui`

**Supersedes:** `ADR-0005 — Strategic Design: Bounded Contexts Definition`

---

### **Context**

ADR-0005 defined four primary Bounded Contexts for the Digital Wallet API: Account (The Identity), Wallet (The Vault), Transaction (The Engine), and Notification (The Messenger).

During MVP Phase 1 implementation, the **Audit Module** was introduced to satisfy the MVP requirement for an immutable audit trail of all financial movements. The Audit module consumes `TransactionCompletedEvent` and produces permanent, queryable audit entries. It owns its own schema (`audit_schema`), its own domain model (`AuditEntry`), its own persistence layer, and exposes a dedicated REST endpoint (`GET /api/audit`).

This module clearly satisfies the criteria for a Bounded Context: it has its own Ubiquitous Language ("audit entry", "event type", "occurred at"), its own data sovereignty (no shared tables), and its own lifecycle independent of the other four contexts.

ADR-0005 was not updated at the time of implementation, leaving a gap between the documented architecture and the actual codebase.

---

### **Decision**

We formally recognize **five** primary Bounded Contexts in the Digital Wallet API:

1.  **Account Context (The "Identity"):**
    * **Responsibility:** Manages user profiles, KYC status, and core account attributes.
    * **Key Aggregate:** `Account`.
    * **Schema:** `account_schema`

2.  **Wallet/Balance Context (The "Vault"):**
    * **Responsibility:** Manages financial balances and ledger integrity. Ensures that money is never created or destroyed without a trace.
    * **Key Aggregate:** `Wallet`.
    * **Schema:** `wallet_schema`

3.  **Transaction Context (The "Engine"):**
    * **Responsibility:** Orchestrates money movement (P2P transfers). Handles the business rules of a transfer, including idempotency and the CQRS statement projection.
    * **Key Aggregate:** `Transaction`.
    * **Schema:** `transaction_schema`

4.  **Audit Context (The "Record"):**
    * **Responsibility:** Maintains an immutable, append-only audit trail of all financial movements. Purely reactive — consumes domain events and persists audit entries. Provides query capabilities by account and time range.
    * **Key Entity:** `AuditEntry`.
    * **Schema:** `audit_schema`

5.  **Notification Context (The "Messenger"):**
    * **Responsibility:** Handles external communication. Purely reactive and triggered by events from other modules. Has no persistent state.
    * **Schema:** None (stateless event consumer).

---

### **Consequences**

-   **Data Sovereignty:** Each of the five modules owns its specific database schema. No module is allowed to join tables from another module's schema.
-   **Ubiquitous Language:** Each context maintains its own domain vocabulary. An "entry" means different things in Audit (`AuditEntry` — immutable record of a financial event), Wallet (`LedgerEntry` — balance mutation), and Transaction (`StatementEntry` — CQRS read projection).
-   **Integration:** The Audit and Notification modules are both **purely reactive** — they consume `TransactionCompletedEvent` and have no upstream callers. They share this trait but serve different purposes: Audit produces a permanent queryable record; Notification produces a transient alert.
-   **Event Fan-Out:** `TransactionCompletedEvent` now has three independent consumers (Statement Projection, Audit, Notification). Each handler must be idempotent to handle at-least-once delivery from the Event Publication Registry.
-   **Future Extraction:** The Audit module's clear boundary makes it a strong candidate for early microservice extraction, as it has zero synchronous dependencies on other modules (event-only input, REST-only output).
