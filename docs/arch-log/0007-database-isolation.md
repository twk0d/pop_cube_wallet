## **Database Isolation: Schema-per-Module**

**Date:** `2026-01-26`

**Status:** `Accepted`

**Reviewer:** `Koda Turqui`

---


### **Context**
To enable a seamless transition to microservices, modules must not share database tables or schemas. Cross-module joins are strictly forbidden.

---


### **Decision**
Each Bounded Context will have its own dedicated PostgreSQL Schema. 
- `account_module` -> schema: `account_schema` (tables: `accounts`)
- `wallet_module` -> schema: `wallet_schema` (tables: `wallets`, `ledger_entries`)
- `transaction_module` -> schema: `transaction_schema` (tables: `transactions`, `statement_entries`)
- `audit_module` -> schema: `audit_schema` (tables: `audit_entries`)

> **Note:** The `notification` module has no dedicated schema. It is a pure event listener that consumes domain events and dispatches alerts without persisting its own state.

---


### **Consequences**
- No Foreign Keys between modules.
- Data consistency across modules must be handled via Domain Events.
- Infrastructure layer must specify the schema in entities or via data source configuration.
- Flyway migrations are configured with `spring.flyway.schemas` listing all module schemas.