package org.jbelt.module;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that H2 (dev) and PostgreSQL (prod) Flyway migrations
 * produce structurally equivalent schemas.
 */
class SchemaComparisonTests {

    @Test
    void devAndProdMigrationsShouldHaveSameVersions() throws IOException {
        Path h2Dir = new ClassPathResource("db/migration/h2").getFile().toPath();
        Path pgDir = new ClassPathResource("db/migration/postgresql").getFile().toPath();

        Set<String> h2Structural = new TreeSet<>();
        Set<String> pgStructural = new TreeSet<>();

        Files.list(h2Dir)
                .filter(p -> p.getFileName().toString().endsWith(".sql"))
                .filter(p -> p.getFileName().toString().startsWith("V"))
                .filter(p -> isStructuralMigration(p.getFileName().toString()))
                .forEach(p -> h2Structural.add(p.getFileName().toString()));

        Files.list(pgDir)
                .filter(p -> p.getFileName().toString().endsWith(".sql"))
                .filter(p -> p.getFileName().toString().startsWith("V"))
                .filter(p -> isStructuralMigration(p.getFileName().toString()))
                .forEach(p -> pgStructural.add(p.getFileName().toString()));

        assertEquals(pgStructural, h2Structural,
                "Structural migration files should have same names in h2/ and postgresql/ directories");
    }

    @Test
    void v1MigrationsShouldCreateSameTables() throws IOException {
        String h2Sql = readMigration("db/migration/h2/V1__init_schema.sql");
        String pgSql = readMigration("db/migration/postgresql/V1__init_schema.sql");

        Set<String> h2Tables = extractTableNames(h2Sql);
        Set<String> pgTables = extractTableNames(pgSql);

        assertFalse(h2Tables.isEmpty(), "H2 migration should create at least one table");
        assertFalse(pgTables.isEmpty(), "PostgreSQL migration should create at least one table");

        assertEquals(pgTables, h2Tables,
                "H2 and PostgreSQL V1 migrations should create the same tables");
    }

    private String readMigration(String classpath) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpath);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private Set<String> extractTableNames(String sql) {
        Set<String> tables = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Pattern pattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            tables.add(matcher.group(1).toLowerCase());
        }
        return tables;
    }

    private boolean isStructuralMigration(String filename) {
        return !filename.toLowerCase().contains("seed");
    }
}
