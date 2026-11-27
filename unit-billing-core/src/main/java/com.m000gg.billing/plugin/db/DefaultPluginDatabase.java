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

import com.m000gg.plugin.api.db.PluginDatabase;
import com.m000gg.plugin.api.db.PluginDatabaseException;
import com.m000gg.plugin.api.db.RowMapper;
import com.m000gg.plugin.api.db.RowView;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Schema-scoped {@link PluginDatabase} backed by the host's PostgreSQL pool.
 *
 * <p>Every statement is validated against {@link #schemaName} before execution
 * and runs inside a transaction after {@code SET LOCAL search_path}.
 */
public class DefaultPluginDatabase implements PluginDatabase {

    private final String schemaName;
    private final String setSearchPathSql;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final PluginSqlValidator sqlValidator;

    public DefaultPluginDatabase(
            String schemaName,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            PluginSqlValidator sqlValidator
    ) {
        this.schemaName = Objects.requireNonNull(schemaName, "schemaName must not be null");
        this.setSearchPathSql = "SET LOCAL search_path TO \"" + schemaName + "\"";
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate must not be null");
        this.sqlValidator = Objects.requireNonNull(sqlValidator, "sqlValidator must not be null");
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        sqlValidator.validateRuntimeSql(sql, schemaName);
        Object[] safeParams = params == null ? new Object[0] : params;

        try {
            return transactionTemplate.execute(status -> {
                jdbcTemplate.execute(setSearchPathSql);
                return jdbcTemplate.query(
                        sql,
                        ps -> bindParams(ps, safeParams),
                        (rs, rowNum) -> mapper.map(new JdbcRowView(rs))
                );
            });
        } catch (PluginDatabaseException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new PluginDatabaseException("Plugin query failed", e);
        }
    }

    @Override
    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        if (rows.size() > 1) {
            throw new PluginDatabaseException("Expected at most 1 row, got " + rows.size());
        }
        return Optional.ofNullable(rows.getFirst());
    }

    @Override
    public int execute(String sql, Object... params) {
        sqlValidator.validateRuntimeSql(sql, schemaName);
        Object[] safeParams = params == null ? new Object[0] : params;

        try {
            Integer updated = transactionTemplate.execute(status -> {
                jdbcTemplate.execute(setSearchPathSql);
                return jdbcTemplate.update(sql, ps -> bindParams(ps, safeParams));
            });
            return updated != null ? updated : 0;
        } catch (PluginDatabaseException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new PluginDatabaseException("Plugin execute failed", e);
        }
    }

    @Override
    public <T> T inTransaction(Supplier<T> action) {
        Objects.requireNonNull(action, "action must not be null");
        try {
            return transactionTemplate.execute(status -> {
                jdbcTemplate.execute(setSearchPathSql);
                return action.get();
            });
        } catch (PluginDatabaseException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new PluginDatabaseException("Plugin transaction failed", e);
        }
    }

    @Override
    public void inTransaction(Runnable action) {
        Objects.requireNonNull(action, "action must not be null");
        inTransaction(() -> {
            action.run();
            return null;
        });
    }

    private static void bindParams(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof Instant instant) {
                ps.setTimestamp(i + 1, Timestamp.from(instant));
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    private static final class JdbcRowView implements RowView {
        private final ResultSet rs;

        private JdbcRowView(ResultSet rs) {
            this.rs = rs;
        }

        @Override
        public String getString(String column) {
            try {
                return rs.getString(column);
            } catch (SQLException e) {
                throw new PluginDatabaseException("Failed to read string column '" + column + "'", e);
            }
        }

        @Override
        public Integer getInt(String column) {
            try {
                int value = rs.getInt(column);
                return rs.wasNull() ? null : value;
            } catch (SQLException e) {
                throw new PluginDatabaseException("Failed to read int column '" + column + "'", e);
            }
        }

        @Override
        public Long getLong(String column) {
            try {
                long value = rs.getLong(column);
                return rs.wasNull() ? null : value;
            } catch (SQLException e) {
                throw new PluginDatabaseException("Failed to read long column '" + column + "'", e);
            }
        }

        @Override
        public Boolean getBoolean(String column) {
            try {
                boolean value = rs.getBoolean(column);
                return rs.wasNull() ? null : value;
            } catch (SQLException e) {
                throw new PluginDatabaseException("Failed to read boolean column '" + column + "'", e);
            }
        }

        @Override
        public BigDecimal getBigDecimal(String column) {
            try {
                return rs.getBigDecimal(column);
            } catch (SQLException e) {
                throw new PluginDatabaseException("Failed to read decimal column '" + column + "'", e);
            }
        }

        @Override
        public UUID getUuid(String column) {
            try {
                Object value = rs.getObject(column);
                return switch (value) {
                    case null -> null;
                    case UUID uuid -> uuid;
                    case String s -> UUID.fromString(s);
                    default -> throw new PluginDatabaseException(
                            "Column '" + column + "' is not a UUID: " + value.getClass().getName());
                };
            } catch (SQLException e) {
                throw new PluginDatabaseException("Failed to read UUID column '" + column + "'", e);
            }
        }

        @Override
        public Instant getInstant(String column) {
            try {
                Timestamp timestamp = rs.getTimestamp(column);
                return timestamp != null ? timestamp.toInstant() : null;
            } catch (SQLException e) {
                throw new PluginDatabaseException("Failed to read timestamp column '" + column + "'", e);
            }
        }
    }
}
