package com.edu.api.pop_cube_wallet;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies that the modular monolith architecture passes Spring Modulith's
 * structural validation (ADR-0002, ADR-0004, ADR-0005).
 * This test is part of the Definition of Done:
 *   "Architecture passes ApplicationModules.of(Application.class).verify()"
 */
class ModularityTests {

    @Test
    void verifyModularStructure() {
        ApplicationModules modules = ApplicationModules.of(PopCubeWalletApplication.class);
        modules.verify();
    }

    @Test
    void generateDocumentation() {
        ApplicationModules modules = ApplicationModules.of(PopCubeWalletApplication.class);
        new Documenter(modules).writeDocumentation();
    }
}
