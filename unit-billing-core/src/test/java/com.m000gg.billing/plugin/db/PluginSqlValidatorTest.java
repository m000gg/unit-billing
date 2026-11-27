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

import com.m000gg.plugin.api.db.PluginDatabaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PluginSqlValidatorTest {

    private static final String ALLOWED_SCHEMA = "plugin_demo";
    private final PluginSqlValidator validator = new PluginSqlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT id, name FROM subscriptions WHERE active = ?",
            "SELECT s.id FROM plugin_demo.subscriptions s JOIN plugin_demo.invoices i ON i.sub_id = s.id",
            "WITH active AS (SELECT id FROM subscriptions WHERE active = true) SELECT id FROM active",
            "INSERT INTO subscriptions (id, name) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name",
            "INSERT INTO logs (message) VALUES ('core.application_users changed in public.admins')",
            "INSERT INTO logs (message) VALUES ($$core.admins$$)",
            "-- core.application_users\nSELECT id FROM subscriptions /* public.admins */",
            "UPDATE subscriptions SET status = ? WHERE id = ?",
            "DELETE FROM subscriptions WHERE id = ?;"
    })
    void acceptsValidRuntimeQueries(String sql) {
        assertDoesNotThrow(() -> validator.validateRuntimeSql(sql, ALLOWED_SCHEMA));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM core.application_users",
            "SELECT * FROM \"core\".\"application_users\"",
            "SELECT * FROM core . admins",
            "SELECT * FROM public.ledger_entries",
            "SELECT * FROM plugin_other.subscriptions",
            "SELECT * FROM evil_schema.subscriptions",
            "SELECT u.id FROM subscriptions s, core.application_users u",
            "SELECT s.id FROM subscriptions s JOIN core.application_users u ON u.id = s.user_id",
            "INSERT INTO core.system_settings (key, setting_value) VALUES ('a', 'b')",
            "UPDATE core.application_users SET balance = 99999",
            "DELETE FROM public.admins",
            "SELECT * FROM pg_catalog.pg_tables",
            "SELECT * FROM information_schema.tables",
            "SELECT * FROM db.plugin_demo.subscriptions",
            "SET search_path TO core",
            "RESET ALL",
            "DO $$ BEGIN PERFORM 1; END $$",
            "DROP TABLE subscriptions",
            "SELECT 1; DELETE FROM core.admins",
            "SELECT set_config('search_path', 'core', true)",
            "SELECT pg_read_file('/etc/passwd')",
            "SELECT dblink('host=localhost', 'select 1')"
    })
    void rejectsCrossSchemaOrDangerousRuntimeQueries(String sql) {
        assertThrows(PluginDatabaseException.class,
                () -> validator.validateRuntimeSql(sql, ALLOWED_SCHEMA));
    }

    @Test
    void acceptsValidMigrationSql() {
        String sql = """
                CREATE TABLE IF NOT EXISTS subscriptions (
                    id UUID PRIMARY KEY,
                    user_id UUID NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE
                );
                CREATE INDEX idx_subscriptions_user ON plugin_demo.subscriptions (user_id);
                ALTER TABLE subscriptions ADD COLUMN note TEXT;
                """;

        assertDoesNotThrow(() -> validator.validateMigrationSql(sql, ALLOWED_SCHEMA));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CREATE TABLE core.hack (id INT);",
            "CREATE TABLE IF NOT EXISTS public.hack (id INT);",
            "ALTER TABLE core.application_users ADD COLUMN evil TEXT;",
            "DELETE FROM core.admins;",
            "CREATE TABLE foo (id UUID REFERENCES core.application_users(id));",
            "CREATE INDEX idx_hack ON core.admins (id);",
            "SET search_path TO core;",
            "DO $$ BEGIN DELETE FROM core.admins; END $$;",
            "CREATE FUNCTION steal() RETURNS void AS $$ SELECT 1; $$ LANGUAGE SQL;",
            "GRANT ALL ON SCHEMA core TO PUBLIC;"
    })
    void rejectsCrossSchemaOrPrivilegedMigrationSql(String sql) {
        assertThrows(PluginDatabaseException.class,
                () -> validator.validateMigrationSql(sql, ALLOWED_SCHEMA));
    }
}
