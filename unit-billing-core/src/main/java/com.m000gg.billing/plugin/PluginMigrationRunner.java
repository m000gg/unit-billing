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

package com.m000gg.billing.plugin;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Applies the SQL migrations shipped inside a plugin JAR.
 *
 * <p>Each plugin gets its own schema ({@code plugin_<id>}) that also holds its own
 * Flyway history table. Migrations are read from {@code plugin-db/migration} of the
 * plugin class loader. The folder must differ from the core's {@code db/migration},
 * because the core classes are visible from the plugin class loader.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginMigrationRunner {

    private static final String MIGRATION_LOCATION = "classpath:plugin-db/migration";
    private static final String SCHEMA_PREFIX = "plugin_";
    private static final int MAX_SCHEMA_NAME_LENGTH = 63;

    private final DataSource dataSource;

    /**
     * @return number of migrations applied; 0 when the plugin has no migrations
     *         or everything is already applied
     */
    public int migrate(String pluginId, ClassLoader pluginClassLoader) {
        String schema = schemaName(pluginId);

        Flyway flyway = Flyway.configure(pluginClassLoader)
                .dataSource(dataSource)
                .locations(MIGRATION_LOCATION)
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .cleanDisabled(true)
                .outOfOrder(false)
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .load();

        if (flyway.info().all().length == 0) {
            log.debug("Plugin '{}' has no migrations", pluginId);
            return 0;
        }

        MigrateResult result = flyway.migrate();
        log.info("Plugin '{}': {} migration(s) applied in schema '{}'",
                pluginId, result.migrationsExecuted, schema);
        return result.migrationsExecuted;
    }

    static String schemaName(String pluginId) {
        PluginManager.validatePluginId(pluginId);

        String schema = SCHEMA_PREFIX + pluginId.replace('-', '_');
        if (schema.length() > MAX_SCHEMA_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Plugin id is too long for a schema name: " + pluginId);
        }
        return schema;
    }
}