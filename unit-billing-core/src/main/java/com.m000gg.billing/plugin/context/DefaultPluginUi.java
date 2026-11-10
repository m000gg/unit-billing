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

import com.m000gg.plugin.api.ui.ClientDashboardSection;
import com.m000gg.plugin.api.ui.PluginPageHandler;
import com.m000gg.plugin.api.ui.PluginUi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultPluginUi implements PluginUi {

    private volatile PluginPageHandler adminPageHandler;
    private volatile PluginPageHandler clientPageHandler;
    private volatile AdminNavigation adminNavigation;

    private final List<ClientDashboardSection> clientDashboardSections =
            new CopyOnWriteArrayList<>();

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

    @Override
    public synchronized void registerClientDashboardSection(
            ClientDashboardSection section
    ) {
        Objects.requireNonNull(
                section,
                "Dashboard section must not be null"
        );

        if (section.getId() == null || section.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Dashboard section ID must not be null or blank"
            );
        }

        if (section.getTitle() == null || section.getTitle().isBlank()) {
            throw new IllegalArgumentException(
                    "Dashboard section title must not be null or blank"
            );
        }

        if (section.getTemplate() == null || section.getTemplate().isBlank()) {
            throw new IllegalArgumentException(
                    "Dashboard section template must not be null or blank"
            );
        }

        boolean alreadyRegistered = clientDashboardSections.stream()
                .anyMatch(existing -> existing.getId().equals(section.getId()));

        if (alreadyRegistered) {
            throw new IllegalArgumentException(
                    "Dashboard section with ID '" + section.getId()
                            + "' is already registered"
            );
        }

        clientDashboardSections.add(section);
    }

    public List<ClientDashboardSection> getClientDashboardSections() {
        return List.copyOf(clientDashboardSections);
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
