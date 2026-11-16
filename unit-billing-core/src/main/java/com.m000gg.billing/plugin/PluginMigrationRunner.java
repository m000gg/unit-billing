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


import com.m000gg.billing.plugin.db.PluginSqlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Applies the SQL migrations shipped inside a plugin JAR.
 *
 * <p>Each plugin gets its own schema ({@code plugin_<id>}) that also holds its own
 * Flyway history table. Migrations are read from {@code plugin-db/migration} of the
 * plugin class loader and validated against {@code plugin_<id>} before execution.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginMigrationRunner {

    private static final String MIGRATION_LOCATION = "classpath:plugin-db/migration";
    private static final String MIGRATION_RESOURCE_PATTERN = "classpath*:plugin-db/migration/**/*.sql";
    private static final String SCHEMA_PREFIX = "plugin_";
    private static final int MAX_SCHEMA_NAME_LENGTH = 63;

    private final DataSource dataSource;
    private final PluginSqlValidator sqlValidator;

    /**
     * @return number of migrations applied; 0 when the plugin has no migrations
     *         or everything is already applied
     */
    public int migrate(String pluginId, ClassLoader pluginClassLoader) {
        String schema = schemaName(pluginId);
        validateMigrationResources(pluginId, schema, pluginClassLoader);

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

    private static final String MIGRATION_PREFIX = "plugin-db/migration/";

    private void validateMigrationResources(
            String pluginId,
            String schema,
            ClassLoader pluginClassLoader
    ) {
        try {
            if (pluginClassLoader instanceof URLClassLoader urlClassLoader) {
                for (URL url : urlClassLoader.getURLs()) {
                    validateUrlMigrations(pluginId, schema, url);
                }
                return;
            }

            PathMatchingResourcePatternResolver resolver =
                    new PathMatchingResourcePatternResolver(pluginClassLoader);
            Resource[] resources = resolver.getResources(MIGRATION_RESOURCE_PATTERN);
            for (Resource resource : resources) {
                validateMigrationSql(
                        pluginId,
                        schema,
                        resource.getFilename(),
                        resource.getContentAsString(StandardCharsets.UTF_8)
                );
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(
                    "Failed to read migrations for plugin '" + pluginId + "'", e);
        }
    }

    private void validateUrlMigrations(String pluginId, String schema, URL url)
            throws IOException, URISyntaxException {
        if (!"file".equalsIgnoreCase(url.getProtocol())) {
            return;
        }
        Path path = Path.of(url.toURI());
        if (Files.isRegularFile(path)) {
            try (JarFile jarFile = new JarFile(path.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory()
                            && entry.getName().startsWith(MIGRATION_PREFIX)
                            && entry.getName().endsWith(".sql")) {
                        try (InputStream in = jarFile.getInputStream(entry)) {
                            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                            validateMigrationSql(pluginId, schema, entry.getName(), sql);
                        }
                    }
                }
            }
        } else if (Files.isDirectory(path)) {
            Path migrationDir = path.resolve(MIGRATION_PREFIX);
            if (Files.isDirectory(migrationDir)) {
                try (Stream<Path> walk = Files.walk(migrationDir)) {
                    for (Path file : walk.filter(Files::isRegularFile).toList()) {
                        if (file.getFileName().toString().endsWith(".sql")) {
                            validateMigrationSql(
                                    pluginId,
                                    schema,
                                    file.getFileName().toString(),
                                    Files.readString(file, StandardCharsets.UTF_8)
                            );
                        }
                    }
                }
            }
        }
    }

    private void validateMigrationSql(String pluginId, String schema, String name, String sql) {
        try {
            sqlValidator.validateMigrationSql(sql, schema);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Invalid migration '" + name + "' in plugin '"
                            + pluginId + "': " + e.getMessage(),
                    e
            );
        }
    }

    public static String schemaName(String pluginId) {
        PluginManager.validatePluginId(pluginId);

        String schema = SCHEMA_PREFIX + pluginId.replace('-', '_');
        if (schema.length() > MAX_SCHEMA_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Plugin id is too long for a schema name: " + pluginId);
        }
        return schema;
    }
}