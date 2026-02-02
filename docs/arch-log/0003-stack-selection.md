## **Technology Stack Selection: Java with GraalVM**

**Date:** `2026-01-26`

**Status:** `Accepted`

**Reviewer:** `Koda Turqui`

---
### **Context**

As the project is a monolith, a single primary technology stack (platform and language) must be selected for the entire implementation to ensure consistency and maintainability.

--- 
### **Decision**

We will use **Java with GraalVM 25** as the runtime platform, **Gradle** as the build tool, and **Spring Framework 4.0.2** as the primary application framework. This stack was chosen for its optimal combination of performance, developer productivity, and robust ecosystem support.

The key factors influencing this decision are:

- **Mature and Supported Ecosystem:** Java is one of the most established programming languages with decades of enterprise adoption. Spring Framework provides a comprehensive programming model with extensive documentation, proven patterns, and a vast ecosystem of libraries and integrations, which reduces development risk and accelerates delivery.
- **Exceptional Performance:** GraalVM 25 delivers superior performance through advanced optimizations including ahead-of-time (AOT) compilation, faster startup times, and lower memory footprint. This makes it ideal for a monolith designed to handle significant load efficiently while reducing operational costs.
- **Cross-Platform Capability:** Java's "write once, run anywhere" philosophy combined with GraalVM's native image capabilities provides critical flexibility for both development and deployment. This allows us to choose the most cost-effective and suitable hosting environment, including containerized platforms like Docker, without vendor lock-in.
- **Developer Productivity:** Java is a mature, type-safe, object-oriented language with excellent tooling support. Combined with Gradle's flexible build system and Spring's dependency injection and auto-configuration features, it enables the team to build and maintain complex business logic efficiently with reduced boilerplate code.
- **Native Image Support:** GraalVM's ability to compile Java applications to native executables provides faster startup times and lower memory consumption, which is particularly beneficial for microservices and cloud-native deployments.
- **Polyglot Programming:** GraalVM's polyglot capabilities allow seamless integration of other languages (JavaScript, Python, R, Ruby) within Java applications, providing flexibility to use the best language for specific tasks or leverage existing libraries from other ecosystems.

---

### **Consequences**

- The entire application will be implemented using Java on the GraalVM 25 runtime.
- Gradle will be used as the build automation tool for dependency management, compilation, and packaging. Its multi-project build capabilities will support our modular monolith architecture, allowing each module to be developed, tested, and managed independently while maintaining isolation between modules.
- Spring Framework will provide the foundational application structure, dependency injection, and integration capabilities.
- The application can be developed, tested, and deployed across Windows, macOS, and Linux environments.
- GraalVM's polyglot capabilities enable the integration of other programming languages within the Java codebase when specific use cases benefit from alternative language features or existing libraries.
- Native image compilation options will be available for optimized deployment scenarios.