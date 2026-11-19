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

package com.m000gg.plugin.api.db;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only view of the current result row, decoupling plugins from JDBC types.
 *
 * <p>All getters return {@code null} when the underlying SQL column value is {@code NULL}.
 */
public interface RowView {
    String getString(String column);

    Integer getInt(String column);

    Long getLong(String column);

    Boolean getBoolean(String column);

    BigDecimal getBigDecimal(String column);

    UUID getUuid(String column);

    Instant getInstant(String column);
}
