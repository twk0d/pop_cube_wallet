# 📨 Domain Event Catalog

This document lists every domain event in the Digital Wallet API, its payload, producers, consumers, delivery guarantees, and idempotency strategies.

---

## Event Publication Registry

All events are dispatched through **Spring Modulith's Event Publication Registry** backed by a JDBC publication log (`EVENT_PUBLICATION` table).

| Property | Value |
|----------|-------|
| **Delivery guarantee** | At-least-once |
| **Persistence** | Publication written inside the producer's `@Transactional` boundary |
| **Completion** | Marked complete only after each `@ApplicationModuleListener` returns successfully |
| **Retry** | Incomplete publications are automatically retried on application restart |
| **Listener annotation** | `@ApplicationModuleListener` (all handlers — no raw `@EventListener` is used) |

Configuration in `application.properties`:

```properties
spring.modulith.events.jdbc-schema-initialization.enabled=true
```

Because delivery is at-least-once, every consumer that persists data **must** implement idempotency guards.

---

## Events

### `AccountCreatedEvent`

Published when a new customer account is successfully persisted.

#### Payload

```java
package com.edu.api.pop_cube_wallet.account;

public record AccountCreatedEvent(
    UUID   accountId,
    String fullName
) {}
```

| Field | Type | Description |
|-------|------|-------------|
| `accountId` | `UUID` | Primary key of the newly created account |
| `fullName` | `String` | Account holder's display name |

#### Producer

| Module | Class | Trigger |
|--------|-------|---------|
| **Account** | `AccountCommandHandler` | End of `execute(CreateAccountCommand)` inside `@Transactional` |

#### Consumers

| # | Module | Class | Action | Idempotency |
|---|--------|-------|--------|-------------|
| 1 | **Wallet** | `WalletEventHandler` | Creates a zero-balance `Wallet` for the new account | `walletRepository.existsByAccountId(accountId)` — skips if wallet already exists |

#### Flow

```text
Account module                          Wallet module
┌──────────────────────┐                ┌──────────────────────────────┐
│ AccountCommandHandler│                │        WalletEventHandler    │
│                      │                │                              │
│  save(account)       │                │  onAccountCreated(event)     │
│  publishEvent(       │──────────────▶ │    if !exists(accountId):    │
│    AccountCreated    │   at-least-    │      Wallet.create(accountId)│
│      Event)          │     once       │      walletRepository.save() │
└──────────────────────┘                └──────────────────────────────┘
```

---

### `TransactionCompletedEvent`

Published when a peer-to-peer transfer completes successfully (both wallets debited/credited).

#### Payload

```java
package com.edu.api.pop_cube_wallet.transaction;

public record TransactionCompletedEvent(
    UUID          transactionId,
    UUID          sourceAccountId,
    UUID          destinationAccountId,
    BigDecimal    amount,
    String        description,
    LocalDateTime completedAt
) {}
```

| Field | Type | Description |
|-------|------|-------------|
| `transactionId` | `UUID` | Unique transaction identifier |
| `sourceAccountId` | `UUID` | Account that was debited |
| `destinationAccountId` | `UUID` | Account that was credited |
| `amount` | `BigDecimal` | Transfer amount (always positive) |
| `description` | `String` | User-supplied transfer description |
| `completedAt` | `LocalDateTime` | UTC timestamp of completion |

#### Producer

| Module | Class | Trigger |
|--------|-------|---------|
| **Transaction** | `TransactionCommandHandler` | End of the P2P transfer flow inside `@Transactional` |

#### Consumers

| # | Module | Class | Action | Idempotency |
|---|--------|-------|--------|-------------|
| 1 | **Transaction** (CQRS read side) | `StatementProjectionHandler` | Creates **2** `StatementEntry` rows: one `SENT` (source) and one `RECEIVED` (destination) | `statementRepository.existsByTransactionIdAndAccountId(txId, accountId)` — skips if entries exist |
| 2 | **Audit** | `AuditEventHandler` | Creates **2** immutable `AuditEntry` rows: one `P2P_TRANSFER_SENT` and one `P2P_TRANSFER_RECEIVED` | `auditEntryRepository.existsByAccountIdAndEventTypeAndOccurredAt(accountId, eventType, occurredAt)` — per-entry duplicate guard |
| 3 | **Notification** | `NotificationListener` | Logs a structured transfer alert (mock in MVP — would be SMS/email/push in production) | None required — stateless log output |

#### Flow

```text
Transaction module                       Transaction module (CQRS read side)
┌────────────────────────────┐           ┌──────────────────────────────────┐
│ TransactionCommandHandler  │           │   StatementProjectionHandler     │
│                            │     ┌────▶│     2× StatementEntry (SENT,    │
│  debit(source)             │     │     │          RECEIVED)              │
│  credit(destination)       │     │     └──────────────────────────────────┘
│  save(transaction)         │     │
│  publishEvent(             │─────┤     Audit module
│    TransactionCompleted    │     │     ┌──────────────────────────────────┐
│      Event)                │     ├────▶│   AuditEventHandler              │
└────────────────────────────┘     │     │     2× AuditEntry (DEBIT_SENT,  │
                                   │     │          CREDIT_RECEIVED)       │
                              at-least-  └──────────────────────────────────┘
                                once
                                   │     Notification module
                                   │     ┌──────────────────────────────────┐
                                   └────▶│   NotificationListener           │
                                         │     log transfer alert (mock)   │
                                         └──────────────────────────────────┘
```

---

## Idempotency Summary

Because the Event Publication Registry guarantees at-least-once delivery, a consumer may receive the same event more than once (e.g., after a crash before the publication was marked complete). Each persistent consumer guards against duplicates:

| Consumer | Guard query | Dedup key |
|----------|-------------|-----------|
| `WalletEventHandler` | `existsByAccountId(accountId)` | `accountId` |
| `StatementProjectionHandler` | `existsByTransactionIdAndAccountId(txId, accountId)` | `transactionId` + `accountId` |
| `AuditEventHandler` | `existsByAccountIdAndEventTypeAndOccurredAt(...)` | `accountId` + `eventType` + `occurredAt` |
| `NotificationListener` | *none* | N/A (stateless log) |

---

## Adding a New Event

1. **Define the event record** in the producing module's root package (e.g., `com.edu.api.pop_cube_wallet.mymodule`):

   ```java
   public record MyDomainEvent(UUID entityId, String detail) {}
   ```

   Placing it in the root package makes it part of the module's **public API** — visible to other modules.

2. **Publish** from a `@Transactional` method:

   ```java
   @Transactional
   public void doSomething() {
       // ... business logic ...
       eventPublisher.publishEvent(new MyDomainEvent(id, detail));
   }
   ```

3. **Consume** with `@ApplicationModuleListener` in the subscribing module:

   ```java
   @ApplicationModuleListener
   void onMyDomainEvent(MyDomainEvent event) {
       if (repository.existsByEntityId(event.entityId())) return; // idempotency guard
       // ... handle event ...
   }
   ```

4. **Add the event to this catalog** with payload, producer, consumers, and idempotency strategy.

---

## Related Documents

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Event flow diagrams and delivery guarantee details |
| [ADR-0008](./docs/arch-log/0008-inter-module-communication.md) | Inter-module communication strategy |
| [ADR-0006](./docs/arch-log/0006-cqrs-strategy.md) | CQRS and statement projection rationale |
| [ADR-0010](./docs/arch-log/0010-audit-bounded-context.md) | Audit bounded context and its event consumption |
