package org.jbelt.module;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test to verify the application context loads successfully.
 * Uses the dev profile to test with H2 in-memory database.
 */
@SpringBootTest
@ActiveProfiles("dev")
class ApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the application context loaded successfully.
        // This verifies:
        // - All beans are properly configured
        // - Database connection works (H2 in dev profile)
        // - Flyway migrations run successfully
        // - Security configuration is valid
    }

}
