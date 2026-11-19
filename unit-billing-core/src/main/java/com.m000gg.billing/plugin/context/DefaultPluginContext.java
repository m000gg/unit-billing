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
import com.m000gg.plugin.api.query.fx.ExchangeRateDirectory;
import com.m000gg.plugin.api.query.settings.PluginSettings;
import com.m000gg.plugin.api.query.user.UserDirectory;
import com.m000gg.plugin.api.spi.fx.ExchangeRateProvider;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DefaultPluginContext implements PluginContext {

    private final String pluginId;
    private final List<Consumer<BalanceChanged>> balanceChangedHandlers = new CopyOnWriteArrayList<>();
    private final UserDirectory userDirectory;
    private final PluginSettings pluginSettings;
    private final ExchangeRateDirectory exchangeRateDirectory;
    private final PluginExchangeRateRegistry exchangeRateRegistry;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public DefaultPluginContext(
            String pluginId,
            UserDirectory userDirectory,
            PluginSettings pluginSettings,
            ExchangeRateDirectory exchangeRateDirectory,
            PluginExchangeRateRegistry exchangeRateRegistry
    ) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId must not be null");
        this.userDirectory = userDirectory;
        this.pluginSettings = pluginSettings;
        this.exchangeRateDirectory = exchangeRateDirectory;
        this.exchangeRateRegistry = exchangeRateRegistry;
    }

    @Override
    public void onBalanceChanged(Consumer<BalanceChanged> handler) {
        balanceChangedHandlers.add(Objects.requireNonNull(handler, "handler must not be null"));
    }

    @Override
    public UserDirectory users() {
        return userDirectory;
    }

    @Override
    public PluginSettings settings() {
        return pluginSettings;
    }

    @Override
    public ExchangeRateDirectory exchangeRates() {
        return exchangeRateDirectory;
    }

    @Override
    public void registerExchangeRateProvider(ExchangeRateProvider provider) {
        Objects.requireNonNull(provider, "provider must not be null");
        if (!active.get()) {
            throw new IllegalStateException("Plugin context is no longer active");
        }
        if (exchangeRateRegistry == null) {
            throw new IllegalStateException("Exchange rate registry is not available");
        }
        exchangeRateRegistry.register(pluginId, provider);
    }

    /**
     * Marks this context inactive so a stopping or stopped plugin cannot register new extensions.
     */
    public void deactivate() {
        active.set(false);
    }

    public List<Consumer<BalanceChanged>> getBalanceChangedHandlers() {
        return List.copyOf(balanceChangedHandlers);
    }
}