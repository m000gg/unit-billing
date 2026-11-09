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
import java.util.Objects;

public sealed interface PluginPageResponse {

    /**
     * An HTML fragment that the core wraps into its layout.
     *
     * @param title       page title
     * @param body        HTML fragment; the core inserts it as it is, so the plugin must escape
     *                    every value it puts into it
     * @param stylesheets paths of CSS files inside the plugin's {@code plugin-static/} folder,
     *                    for example {@code /subscriptions.css}
     * @param scripts     paths of JS files inside {@code plugin-static/}
     */
    record Html(String title, String body, List<String> stylesheets, List<String> scripts)
            implements PluginPageResponse {

        public Html {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(body, "body");
            stylesheets = PluginPaths.requireRelativeAll(stylesheets, "stylesheets");
            scripts = PluginPaths.requireRelativeAll(scripts, "scripts");
        }
    }

    record Redirect(String path) implements PluginPageResponse {

        public Redirect {
            PluginPaths.requireRelative(path, "path");
        }
    }

    record Failure(Status status) implements PluginPageResponse {

        public Failure {
            Objects.requireNonNull(status, "status");
        }
    }

    enum Status {
        NOT_FOUND,
        FORBIDDEN
    }

    static PluginPageResponse html(String title, String body) {
        return new Html(title, body, List.of(), List.of());
    }

    static PluginPageResponse html(String title, String body, List<String> stylesheets, List<String> scripts) {
        return new Html(title, body, stylesheets, scripts);
    }

    static PluginPageResponse redirect(String path) {
        return new Redirect(path);
    }

    static PluginPageResponse notFound() {
        return new Failure(Status.NOT_FOUND);
    }

    static PluginPageResponse forbidden() {
        return new Failure(Status.FORBIDDEN);
    }
}
