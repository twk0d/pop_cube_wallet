## **Strategic Design: Bounded Contexts Definition**

**Date:** `2026-01-26`

**Status:** `Accepted`

**Reviewer:** `Koda Turqui`

---

### **Context**

As we move forward with a **Modular Monolith**, we must define the boundaries of each module based on **Domain-Driven Design (DDD)**. Without clear boundaries, the system risks becoming tightly coupled, making future extraction into microservices impossible.

---

### **Decision**

We will divide the Digital Wallet API into four primary **Bounded Contexts**, each mapped to a specific system module:

1.  **Account Context (The "Identity"):**
    * **Responsibility:** Manages user profiles, KYC status, and core account attributes.
    * **Key Aggregate:** `Account`.

2.  **Wallet/Balance Context (The "Vault"):**
    * **Responsibility:** Manages financial balances and ledger integrity. It ensures that money is never created or destroyed without a trace.
    * **Key Aggregate:** `Wallet`.

3.  **Transaction Context (The "Engine"):**
    * **Responsibility:** Orchestrates money movement (P2P, PIX, Deposits). It handles the business rules of a "transfer".
    * **Key Aggregate:** `Transaction`.

4.  **Notification Context (The "Messenger"):**
    * **Responsibility:** Handles external communication. It is purely reactive and triggered by events from other modules.

---

### **Consequences**

-   **Data Sovereignty:** Each module owns its specific database tables. No module is allowed to join tables from another module.
-   **Ubiquitous Language:** The term "Account" might mean different things in the Account Context (User Data) vs. the Transaction Context (Financial Destination). Each module will have its own Domain Model.
-   **Integration:** Modules will communicate via **Domain Events** (e.g., `TransactionCreated`) to ensure that the Transaction module doesn't need to know how the Notification module works.