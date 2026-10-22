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

package com.m000gg.plugin.api;

import com.m000gg.plugin.api.event.BalanceChanged;
import com.m000gg.plugin.api.query.settings.PluginSettings;
import com.m000gg.plugin.api.query.user.UserDirectory;

import java.util.function.Consumer;

public interface PluginContext {
    /**
     * The listener is invoked asynchronously after the balance transaction commits.
     * Events are delivered in order for each plugin.
     * Listener failures do not affect the completed transaction or other plugins.
     */
    void onBalanceChanged(Consumer<BalanceChanged> handler);

    UserDirectory users();

    PluginSettings settings();
}
