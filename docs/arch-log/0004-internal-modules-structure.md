## **Internal Module Architecture: Hexagonal (Ports & Adapters)**

**Date:** `2026-01-26`

**Status:** `Accepted`

**Reviewer:** `Koda Turqui`

---

### **Context**

To ensure the **SOLID** principles, we need a standardized internal structure for our modules. 

A fintech API requires high testability and isolation of business rules from external technologies (database, external payment gateways, notification services). We need to choose between traditional layered architecture, Clean Architecture, or Hexagonal Architecture.

---

### **Decision**

We will adopt the **Hexagonal Architecture (Ports & Adapters)** as the internal structure for each DDD Bounded Context within our Modular Monolith.

The implementation will be guided by the following structure:

1.  **Domain Layer (Core):** Contains the business logic, DDD Aggregates, Entities, Value Objects, and **Domain Service Interfaces**. This layer has zero dependencies on frameworks or external libraries (except jMolecules).
2.  **Application Layer (Ports):** Contains the **CQRS Handlers** (Commands/Queries) and **Port Interfaces**.
    * **Input Ports:** Interfaces or Use Cases called by the primary adapters (Web/API).
    * **Output Ports:** Interfaces for data persistence or external communication (Repositories, Gateways).
3.  **Infrastructure Layer (Adapters):** Contains the technical implementation details.
    * **Primary Adapters:** REST Controllers (Spring MVC) and Event Listeners that drive the application.
    * **Secondary Adapters:** Spring Data JPA repositories, Resilience4j implementations, and external API clients.



---

### **Consequences**

-   **Dependency Inversion:** The Domain and Application layers will not depend on the Infrastructure layer. Instead, Infrastructure will depend on the Ports (interfaces) defined in the Application/Domain layers, helping to 'KISS'-it.
-   **Testability:** Business logic can be tested in isolation without the need for Spring Context or database mocks, using pure JUnit 5.
-   **jMolecules Integration:** We will use `@org.jmolecules.architecture.hexagonal.HexagonalArchitecture` annotations to document and enforce these boundaries.
-   **Strict Separation:** Communication between modules must happen through the Application Layer's API or via Spring Modulith Events, never by direct access to another module's Infrastructure layer.
-   **Boilerplate:** There will be a slight increase in the number of classes due to the need for Port interfaces and Mapping (via MapStruct) between layers.