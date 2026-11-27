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

import com.m000gg.billing.plugin.PluginMigrationRunner;
import com.m000gg.plugin.api.db.PluginDatabase;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Creates a schema-scoped {@link PluginDatabase} per loaded plugin.
 */
@Component
@RequiredArgsConstructor
public class PluginDatabaseFactory {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final PluginSqlValidator sqlValidator;

    public PluginDatabase create(String pluginId) {
        String schema = PluginMigrationRunner.schemaName(pluginId);
        return new DefaultPluginDatabase(
                schema,
                jdbcTemplate,
                new TransactionTemplate(transactionManager),
                sqlValidator
        );
    }
}
