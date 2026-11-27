/*
 * Copyright 2026 Vladyslav Livandovskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m000gg.billing.plugin.db;

import com.m000gg.billing.identity.AdminRepository;
import com.m000gg.billing.plugin.PluginMigrationRunner;
import com.m000gg.plugin.api.db.PluginDatabase;
import com.m000gg.plugin.api.db.PluginDatabaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class PluginDatabaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @TempDir
    Path tempDir;

    @Autowired
    private PluginMigrationRunner migrationRunner;

    @Autowired
    private PluginDatabaseFactory pluginDatabaseFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminRepository adminRepository;

    @Test
    void migratesPluginSchemaAndIsolatesQueriesFromCore() throws Exception {
        String validMigration = """
                CREATE TABLE items (
                    id UUID PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    qty INTEGER,
                    counter BIGINT,
                    active BOOLEAN,
                    amount NUMERIC(19, 2),
                    created_at TIMESTAMPTZ
                );
                """;
        Path goodJar = createMigrationJar("good-plugin.jar", "V1__create_items.sql", validMigration);
        try (URLClassLoader cl = new URLClassLoader(new URL[]{goodJar.toUri().toURL()}, null)) {
            int applied = migrationRunner.migrate("demo-plugin", cl);
            assertThat(applied).isEqualTo(1);
        }

        PluginDatabase db = pluginDatabaseFactory.create("demo-plugin");
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-16T21:00:00Z");

        int inserted = db.execute(
                "INSERT INTO items (id, name, qty, counter, active, amount, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, "starter-pack", 3, 42L, true, new BigDecimal("19.99"), now
        );
        assertThat(inserted).isEqualTo(1);

        Optional<ItemRow> loaded = db.queryOne(
                "SELECT id, name, qty, counter, active, amount, created_at FROM items WHERE id = ?",
                row -> new ItemRow(
                        row.getUuid("id"),
                        row.getString("name"),
                        row.getInt("qty"),
                        row.getLong("counter"),
                        row.getBoolean("active"),
                        row.getBigDecimal("amount"),
                        row.getInstant("created_at")
                ),
                id
        );

        assertThat(loaded).contains(new ItemRow(id, "starter-pack", 3, 42L, true, new BigDecimal("19.99"), now));

        // Verify SQL NULL maps cleanly to Java null (not 0/false)
        UUID nullId = UUID.randomUUID();
        db.execute("INSERT INTO items (id, name) VALUES (?, ?)", nullId, "nullable");
        ItemRow nullRow = db.queryOne(
                "SELECT id, name, qty, counter, active, amount, created_at FROM items WHERE id = ?",
                row -> new ItemRow(
                        row.getUuid("id"),
                        row.getString("name"),
                        row.getInt("qty"),
                        row.getLong("counter"),
                        row.getBoolean("active"),
                        row.getBigDecimal("amount"),
                        row.getInstant("created_at")
                ),
                nullId
        ).orElseThrow();
        assertThat(nullRow.qty()).isNull();
        assertThat(nullRow.counter()).isNull();
        assertThat(nullRow.active()).isNull();
        assertThat(nullRow.amount()).isNull();
        assertThat(nullRow.createdAt()).isNull();

        // Verify rollback inside inTransaction
        UUID rolledBackId = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> db.inTransaction(() -> {
            db.execute("INSERT INTO items (id, name) VALUES (?, ?)", rolledBackId, "temp");
            throw new IllegalStateException("boom");
        }));
        assertThat(db.query("SELECT id FROM items WHERE id = ?", row -> row.getUuid("id"), rolledBackId))
                .isEmpty();

        // Verify native queries targeting core or changing search_path are rejected
        assertThrows(PluginDatabaseException.class, () ->
                db.query("SELECT id FROM core.admins", row -> row.getUuid("id")));
        assertThrows(PluginDatabaseException.class, () ->
                db.execute("SET search_path TO core"));

        // Verify host core queries still operate on the core schema
        assertThat(adminRepository.count()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void rejectsPluginMigrationTargetingCoreSchemaBeforeFlywayRuns() throws Exception {
        String evilMigration = "DELETE FROM core.admins;";
        Path badJar = createMigrationJar("evil-plugin.jar", "V1__evil.sql", evilMigration);

        try (URLClassLoader cl = new URLClassLoader(new URL[]{badJar.toUri().toURL()}, null)) {
            assertThrows(IllegalStateException.class, () -> migrationRunner.migrate("evil-plugin", cl));
        }

        Integer schemaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.schemata WHERE schema_name = 'plugin_evil_plugin'",
                Integer.class
        );
        assertThat(schemaCount).isZero();
    }

    private Path createMigrationJar(String jarName, String sqlFileName, String sqlContent) throws IOException {
        Path jarPath = tempDir.resolve(jarName);
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("plugin-db/migration/" + sqlFileName));
            jos.write(sqlContent.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return jarPath;
    }

    private record ItemRow(
            UUID id,
            String name,
            Integer qty,
            Long counter,
            Boolean active,
            BigDecimal amount,
            Instant createdAt
    ) {
    }
}
