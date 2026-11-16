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

/**
 * Unchecked exception thrown when plugin SQL validation fails or a database
 * operation executed through {@link PluginDatabase} encounters an error.
 */
public class PluginDatabaseException extends RuntimeException {

    public PluginDatabaseException(String message) {
        super(message);
    }

    public PluginDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginDatabaseException(Throwable cause) {
        super(cause);
    }
}
