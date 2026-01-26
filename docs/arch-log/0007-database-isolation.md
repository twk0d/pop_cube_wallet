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
- `account_module` -> schema: `account_schema`
- `transaction_module` -> schema: `transaction_schema`

---


### **Consequences**
- No Foreign Keys between modules.
- Data consistency across modules must be handled via Domain Events.
- Infrastructure layer must specify the schema in entities or via data source configuration.