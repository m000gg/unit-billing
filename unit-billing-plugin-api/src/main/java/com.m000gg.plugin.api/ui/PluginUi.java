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


/**
 * Registration of the web interface of a plugin. Use it in {@code Plugin.onEnable}.
 */
public interface PluginUi {

    /**
     * Registers the handler for the pages under {@code /admin/plugins/{pluginId}/}, visible to admins.
     */
    void registerAdminPages(PluginPageHandler handler);

    /**
     * Registers the handler for the pages under {@code /client/plugins/{pluginId}/}, visible to subscribers.
     */
    void registerClientPages(PluginPageHandler handler);

    /**
     * Registers an administrative navigation entry for this plugin.
     *
     * <p>The entry can be displayed in the administration sidebar
     * and on the administration dashboard.
     *
     * <p>The navigation entry is displayed only if the plugin has
     * also registered an administrative page handler.
     *
     * @param title the display name of the navigation entry;
     *              must not be {@code null} or blank
     * @param icon  the icon identifier used by the host application's
     *              UI; must not be {@code null} or blank
     */
    void registerAdminNavigation(String title, String icon);


    /**
     * Registers a section on the client's main dashboard.
     *
     * @param section dashboard section provided by the plugin
     */
    void registerClientDashboardSection(ClientDashboardSection section);


}