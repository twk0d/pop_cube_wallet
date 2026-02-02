## **Communication Pattern: Event-Driven Inter-Module Communication**

**Date:** `2026-01-26`

**Status:** `Accepted`

**Reviewer:** `Koda Turqui`

---

### **Context**

In a **Modular Monolith**, modules must remain autonomous. Direct service injection between modules (e.g., `TransactionService` calling `NotificationService`) creates tight coupling. If the Notification module changes or fails, it directly impacts the Transaction module. We need a way to synchronize data and trigger actions across boundaries without direct dependencies.

---

### **Decision**

We will use **Asynchronous Domain Events** for all inter-module communication that does not require an immediate return value.

1.  **Spring Modulith Events:** We will leverage `ApplicationEventPublisher` to publish events.
2.  **Event Publication Registry:** We will use the Spring Modulith JDBC starter to ensure "at-least-once" delivery. Events will be persisted in the database before being dispatched.
3.  **Asynchronous Listeners:** Subscribers must use `@ApplicationModuleListener` (which is transactional and asynchronous by default) to process events.
4.  **Internal Events only:** Events are part of the module's API. Only events explicitly placed in the `package-info.java` or public packages can be consumed by other modules.

---

### **Consequences**

-   **Loose Coupling:** The Transaction module only knows that a `TransactionCompletedEvent` occurred; it has no knowledge of the Notification module.
-   **Reliability:** If the application crashes after a transaction is saved but before the notification is sent, Spring Modulith will retry sending the event upon restart.
-   **Consistency:** We accept **Eventual Consistency**. The account balance is updated immediately, but the notification might arrive a few seconds later.
-   **Observability:** We can easily track the flow of business processes by monitoring the event registry table.