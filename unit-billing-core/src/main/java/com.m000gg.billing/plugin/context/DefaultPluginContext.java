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

import com.m000gg.plugin.api.PluginContext;
import com.m000gg.plugin.api.event.BalanceChanged;
import com.m000gg.plugin.api.query.settings.PluginSettings;
import com.m000gg.plugin.api.query.user.UserDirectory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DefaultPluginContext implements PluginContext {

    private final List<Consumer<BalanceChanged>> balanceChangedHandlers = new CopyOnWriteArrayList<>();
    private final UserDirectory userDirectory;
    private final PluginSettings pluginSettings;

    public DefaultPluginContext(UserDirectory userDirectory, PluginSettings pluginSettings) {
        this.userDirectory = userDirectory;
        this.pluginSettings = pluginSettings;
    }

    @Override
    public void onBalanceChanged(Consumer<BalanceChanged> handler) {
        balanceChangedHandlers.add(handler);
    }

    @Override
    public UserDirectory users() {
        return userDirectory;
    }

    @Override
    public PluginSettings settings() {
        return pluginSettings;
    }

    public List<Consumer<BalanceChanged>> getBalanceChangedHandlers() {
        return List.copyOf(balanceChangedHandlers);
    }
}