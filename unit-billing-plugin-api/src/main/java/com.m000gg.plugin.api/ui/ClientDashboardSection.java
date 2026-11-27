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


package com.m000gg.plugin.api.ui;

import java.util.List;

/**
 * Represents a section that a plugin contributes
 * to the client's main dashboard.
 *
 * Implementations are registered through PluginUi.
 */
public interface ClientDashboardSection {

    /**
     * Unique identifier of this section within its plugin.
     */
    String getId();

    /**
     * Display title of the dashboard section.
     */
    String getTitle();

    /**
     * Display order. Lower values are displayed first.
     */
    default int getOrder() {
        return 100;
    }

    /**
     * Path to the HTML fragment inside the plugin JAR.
     *
     * Example: templates/client/network-status.html
     */
    String getTemplate();

    /**
     *
     * Get CSS
     */
    default List<String> getStylesheets() {
        return List.of();
    }

    /**
     *
     * Get JS
     */
    default List<String> getScripts() {
        return List.of();
    }
}

