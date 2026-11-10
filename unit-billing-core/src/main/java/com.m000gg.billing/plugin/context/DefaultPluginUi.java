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

package com.m000gg.billing.plugin.context;

import com.m000gg.plugin.api.ui.PluginPageHandler;
import com.m000gg.plugin.api.ui.PluginUi;

import java.util.Objects;
import java.util.Optional;

public class DefaultPluginUi implements PluginUi {

    private volatile PluginPageHandler adminPageHandler;
    private volatile PluginPageHandler clientPageHandler;
    private volatile AdminNavigation adminNavigation;

    @Override
    public void registerAdminPages(PluginPageHandler handler) {
        this.adminPageHandler = Objects.requireNonNull(
                handler,
                "handler must not be null"
        );
    }

    @Override
    public void registerClientPages(PluginPageHandler handler) {
        this.clientPageHandler = Objects.requireNonNull(
                handler,
                "handler must not be null"
        );
    }

    @Override
    public void registerAdminNavigation(String title, String icon) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Navigation title must not be null or blank"
            );
        }

        if (icon == null || icon.isBlank()) {
            throw new IllegalArgumentException(
                    "Navigation icon must not be null or blank"
            );
        }

        this.adminNavigation = new AdminNavigation(title, icon);
    }

    public Optional<PluginPageHandler> getAdminPageHandler() {
        return Optional.ofNullable(adminPageHandler);
    }

    public Optional<PluginPageHandler> getClientPageHandler() {
        return Optional.ofNullable(clientPageHandler);
    }

    public Optional<AdminNavigation> getAdminNavigation() {
        return Optional.ofNullable(adminNavigation);
    }

    public record AdminNavigation(String title, String icon) {
    }
}
