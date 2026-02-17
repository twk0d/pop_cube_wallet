## **Resilience4j Deferred to Microservice Extraction Phase**

**Date:** `2026-02-20`

**Status:** `Accepted`

**Reviewer:** `Koda Turqui`

---

### **Context**

During initial project scaffolding, `spring-cloud-starter-circuitbreaker-resilience4j` and the Spring Cloud BOM were declared in `build.gradle` based on the MVP document's Technical Risks table, which recommended circuit breakers, rate limiters, and retry mechanisms for production hardening.

After implementing all five bounded contexts (Account, Wallet, Transaction, Audit, Notification), it became clear that **all cross-module calls are in-process** within the modular monolith. There is no network boundary, no HTTP hop, and no external service call that could fail independently. `WalletApi` and `AccountApi` are synchronous in-process interfaces resolved by Spring's dependency injection.

Similarly, **MapStruct** (`mapstruct`, `mapstruct-processor`, `lombok-mapstruct-binding`) was declared for domain ↔ JPA entity mapping but never adopted. The domain model uses value objects (`Cpf`, `Email`) and static `reconstitute()` factory methods that are incompatible with MapStruct's default mapping strategy without extensive custom configuration, negating its code-generation benefits.

---

### **Decision**

Remove Resilience4j, the Spring Cloud BOM, and MapStruct from the Phase 1 build. Do not apply circuit breakers, rate limiters, or bulkheads to in-process method calls.

Specifically removed:
- `org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j`
- `org.springframework.cloud:spring-cloud-dependencies` BOM
- `springCloudVersion` ext property
- `org.mapstruct:mapstruct` + `mapstruct-processor`
- `org.projectlombok:lombok-mapstruct-binding`

---

### **Rationale**

- **YAGNI (You Aren't Gonna Need It):** Circuit breakers protect against network failures. With zero network boundaries in a monolith, they add configuration overhead with no resilience benefit.
- **Spring Modulith handles retry internally:** The Event Publication Registry already provides at-least-once delivery with automatic retry on application restart — covering the primary failure scenario (event handler crash).
- **MapStruct adds complexity for DDD models:** Value objects and static factory reconstruction patterns require custom `@Mapping` + `@ObjectFactory` annotations that offset any boilerplate savings. Manual mapping is cleaner and debuggable.
- **Dependency footprint:** Removing these libraries eliminates ~15 MB of transitive dependencies, simplifies the build, and removes the Spring Cloud BOM (which manages versions for dozens of unused libraries).

---

### **Consequences**

**Positive:**
- Simpler `build.gradle` with no Spring Cloud BOM dependency management.
- Reduced artifact size and faster builds (~15 MB fewer transitive dependencies).
- No misleading configuration — developers won't expect Resilience4j patterns in the codebase.
- Manual mappers in persistence adapters are explicit and aligned with hexagonal architecture.

**Negative:**
- When microservice extraction begins (see ADR-0002), Resilience4j must be re-added and configured.
- MapStruct could be reconsidered if the domain model evolves toward flat DTOs without value objects.

---

### **Future: Resilience4j Application Points for Microservice Extraction**

When modules are extracted into independent services (per ADR-0002), the following patterns should be applied:

| Pattern | Target | Rationale |
|---------|--------|-----------|
| **Circuit Breaker** | `WalletApi` calls from Transaction module | Network calls to the Wallet service can timeout or fail; fast-fail prevents cascading failures |
| **Rate Limiter** | `POST /api/transactions/transfer` | Protect the transfer endpoint from abuse; enforce per-user/per-endpoint request caps |
| **Retry + Exponential Backoff** | Event publication handlers (Spring Modulith → Kafka/RabbitMQ) | Replace in-process event retry with a configurable strategy including Dead Letter Queue |
| **Bulkhead** | Notification module async listeners | Isolate notification processing threads to prevent slow consumers from starving the main thread pool |
