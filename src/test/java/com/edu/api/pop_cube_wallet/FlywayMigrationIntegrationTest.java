package com.edu.api.pop_cube_wallet;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that boots a real PostgreSQL container via Testcontainers
 * and validates that all Flyway migrations apply cleanly and produce the
 * expected schema/table structure.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Testcontainers
class FlywayMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("pop_cube_wallet_test")
                    .withUsername("test_user")
                    .withPassword("test_pass");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allMigrationsApplySuccessfully() {
        MigrationInfoService info = flyway.info();
        MigrationInfo[] applied = info.applied();

        // V1 through V5 should all be applied
        assertThat(applied)
                .hasSizeGreaterThanOrEqualTo(5)
                .allSatisfy(m -> assertThat(m.getState().isApplied()).isTrue());

        // No pending or failed migrations
        assertThat(info.pending()).isEmpty();
    }

    @Test
    void expectedTablesExistInCorrectSchemas() {
        Map<String, List<String>> expectedTables = Map.of(
                "account_schema", List.of("accounts"),
                "wallet_schema", List.of("wallets", "ledger_entries"),
                "transaction_schema", List.of("transactions", "statement_entries"),
                "audit_schema", List.of("audit_entries")
        );

        for (var entry : expectedTables.entrySet()) {
            String schema = entry.getKey();
            for (String table : entry.getValue()) {
                Boolean exists = jdbcTemplate.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1 FROM information_schema.tables
                            WHERE table_schema = ? AND table_name = ?
                        )
                        """,
                        Boolean.class,
                        schema, table
                );
                assertThat(exists)
                        .as("Table %s.%s should exist", schema, table)
                        .isTrue();
            }
        }
    }
}
