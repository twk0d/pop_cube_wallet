# ADR-0011 — Spring Boot 4 Migration: Breaking Changes and Resolutions

**Date:** `2026-02-20`

**Status:** `Accepted`

**Reviewer:** `Koda Turqui`

**Relates to:** `ADR-0003 — Stack Selection`

---

## Context

During the build stabilisation phase of MVP Phase 1, upgrading to **Spring Boot 4.0.2** (with Spring Framework 7, Hibernate ORM 7.2, and Jakarta EE 11) exposed five breaking changes across third-party libraries and Spring ecosystem modules. None were documented in a single migration guide at the time. This ADR records each issue, its root cause, and the minimal fix applied so that future projects upgrading to Spring Boot 4 have a ready reference.

---

## Decision

We resolved all five breaking changes as described below. Each fix was validated by a passing `./gradlew clean build` (16 tasks, 5 tests, exit code 0).

---

### 1. springdoc-openapi 2.x → 3.0.0

| Aspect | Detail |
|--------|--------|
| **Symptom** | `processAot` fails with `NoClassDefFoundError: org/springframework/data/util/TypeInformation` |
| **Root cause** | springdoc 2.x depends on Spring Data 3.x APIs (`QuerydslPredicateOperationCustomizer`) that were removed in Spring Data 4.x (shipped with Spring Boot 4) |
| **Fix** | Upgrade `springdoc-openapi-starter-webmvc-ui` from `2.8.6` to `3.0.0` |
| **Notes** | springdoc 3.0.0 targets Spring Boot 4.0.0 (confirmed via its parent POM). The Maven coordinates and annotation API (`@Operation`, `@Schema`, etc.) remain unchanged |

```groovy
// Before (broken)
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6'

// After (fixed)
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0'
```

---

### 2. Flyway auto-configuration extracted to spring-boot-starter-flyway

| Aspect | Detail |
|--------|--------|
| **Symptom** | Flyway migrations never execute; Hibernate `ddl-auto=validate` fails with `SchemaManagementException: missing table [account_schema.accounts]` |
| **Root cause** | In Spring Boot 4, Flyway auto-configuration was extracted from `spring-boot-autoconfigure` into a dedicated `spring-boot-flyway` module. Declaring only `flyway-core` no longer triggers auto-configuration |
| **Fix** | Replace `flyway-core` with `spring-boot-starter-flyway` (which transitively brings `spring-boot-flyway`, `flyway-core`, and `spring-boot-starter-jdbc`) |
| **Notes** | `flyway-database-postgresql` is still required as a separate dependency for PostgreSQL dialect support |

```groovy
// Before (broken — auto-config never activates)
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'

// After (fixed)
implementation 'org.springframework.boot:spring-boot-starter-flyway'
implementation 'org.flywaydb:flyway-database-postgresql'
```

---

### 3. Testcontainers 2.x artifact renaming and BOM requirement

| Aspect | Detail |
|--------|--------|
| **Symptom** | `compileTestJava` fails with `Could not find org.testcontainers:junit-jupiter:` and `org.testcontainers:postgresql:` |
| **Root cause** | Testcontainers 2.x renamed all artifacts with a `testcontainers-` prefix (e.g., `junit-jupiter` → `testcontainers-junit-jupiter`). Additionally, Spring Boot 4 no longer manages Testcontainers versions via its own BOM, so a version must be declared explicitly |
| **Fix** | (a) Add `testcontainers-bom:2.0.2` to `dependencyManagement`, (b) rename artifacts to their 2.x names |

```groovy
// Before (broken)
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:postgresql'

// After (fixed)
testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
testImplementation 'org.testcontainers:testcontainers-postgresql'

// dependencyManagement block
dependencyManagement {
    imports {
        mavenBom 'org.testcontainers:testcontainers-bom:2.0.2'
    }
}
```

**Additional test fix:** `@DynamicPropertySource` with `postgres::getJdbcUrl` is not AOT-compatible (mapped port is resolved before the container starts during `processTestAot`). Replaced with `@ServiceConnection` which is AOT-aware.

```java
// Before (AOT-incompatible)
@DynamicPropertySource
static void overrideDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
}

// After (AOT-compatible)
@Container
@ServiceConnection
static PostgreSQLContainer<?> postgres = ...;
```

---

### 4. jMolecules artifact renaming

| Aspect | Detail |
|--------|--------|
| **Symptom** | `compileJava` fails with `Could not find org.jmolecules:jmolecules-architecture-hexagonal` |
| **Root cause** | The jMolecules 2025.x BOM renamed the hexagonal architecture artifact from `jmolecules-architecture-hexagonal` to `jmolecules-hexagonal-architecture` |
| **Fix** | Update the artifact ID to match the BOM |

```groovy
// Before (broken)
implementation 'org.jmolecules:jmolecules-architecture-hexagonal'

// After (fixed)
implementation 'org.jmolecules:jmolecules-hexagonal-architecture'
```

---

### 5. Spring Modulith `@NamedInterface` for shared subpackages

| Aspect | Detail |
|--------|--------|
| **Symptom** | `ModularityTests.verifyModularStructure()` fails with `Violations: Module 'account' depends on named interface(s) 'shared :: web'` |
| **Root cause** | Spring Modulith treats subpackages of a module as **internal** by default. Types in `shared.web` (e.g., `ApiError`) are not exposed to other modules unless the subpackage is explicitly declared as a named interface. This is not new to Spring Boot 4 but became apparent during modulith verification |
| **Fix** | (a) Create `shared/web/package-info.java` with `@NamedInterface("web")`, (b) add `"shared :: web"` to each consuming module's `allowedDependencies` |

```java
// shared/web/package-info.java
@org.springframework.modulith.NamedInterface("web")
package com.edu.api.pop_cube_wallet.shared.web;
```

```java
// Example: account/package-info.java
@org.springframework.modulith.ApplicationModule(
        displayName = "Account",
        allowedDependencies = {"shared", "shared :: web"}
)
package com.edu.api.pop_cube_wallet.account;
```

---

## Consequences

- **Positive:** The full build (`./gradlew clean build`) passes end-to-end — compilation, AOT processing, and all 5 tests (Flyway integration, modularity verification, context loads, documentation generation).
- **Positive:** This ADR provides a single-document migration checklist for Spring Boot 4 upgrades.
- **Neutral:** The `spring-boot-starter-flyway` starter adds `spring-boot-starter-jdbc` transitively, which was already on the classpath via `spring-boot-starter-data-jpa`.
- **Neutral:** The Hibernate `enableAssociationManagement = true` configuration emits a deprecation warning. It is non-blocking and will be addressed in a future phase.
