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
 * Renders the pages of a plugin. The core calls it for every request to the plugin namespace,
 * after authentication, authorization and CSRF checks.
 * The handler runs on the request thread, so it must be thread-safe and fast. During the call
 * the plugin class loader is the thread context class loader. If the handler throws, the core
 * logs the exception and shows an error page without the stack trace.
 */
@FunctionalInterface
public interface PluginPageHandler {

    PluginPageResponse handle(PluginPageRequest request);
}
