package org.jbelt.module;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Tests that verify the modular structure of the application.
 * These tests ensure module boundaries are respected and
 * dependencies between modules are properly declared.
 */
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void verifiesModularStructure() {
        // This test fails if:
        // - A module accesses another module's internal types
        // - A module depends on another module not in its allowedDependencies
        // - Circular dependencies exist between modules
        modules.verify();
    }

    @Test
    void printsModuleArrangement() {
        // Outputs the detected module structure to console.
        // Useful for debugging and documentation.
        modules.forEach(System.out::println);
    }

}
