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

import com.m000gg.plugin.api.query.fx.ExchangeRate;
import com.m000gg.plugin.api.spi.fx.ExchangeRateProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;

/**
 * Thread-safe core registry for plugin-supplied {@link ExchangeRateProvider}s.
 * Performs no network I/O itself and isolates callers from plugin failures.
 */
@Slf4j
@Component
public class PluginExchangeRateRegistry {

    static final String IDENTITY_SOURCE = "identity";

    private final String configuredProviderPluginId;
    private final Map<String, ExchangeRateProvider> providers = new LinkedHashMap<>();

    @Autowired
    public PluginExchangeRateRegistry(
            @Value("${unitbilling.configuration.plugins.fx-provider:}") String configuredProviderPluginId
    ) {
        this.configuredProviderPluginId =
                configuredProviderPluginId == null ? "" : configuredProviderPluginId.trim();
    }

    public PluginExchangeRateRegistry() {
        this("");
    }

    public synchronized void register(String pluginId, ExchangeRateProvider provider) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId must not be null or blank");
        }
        Objects.requireNonNull(provider, "provider must not be null");

        if (!configuredProviderPluginId.isEmpty() && !configuredProviderPluginId.equals(pluginId)) {
            log.info(
                    "Registered standby FX provider '{}' (active provider is configured as '{}')",
                    pluginId,
                    configuredProviderPluginId
            );
        } else if (configuredProviderPluginId.isEmpty() && !providers.isEmpty() && !providers.containsKey(pluginId)) {
            String activePluginId = providers.keySet().iterator().next();
            log.warn(
                    "FX provider '{}' registered, but '{}' remains active (set unitbilling.configuration.plugins.fx-provider to override)",
                    pluginId,
                    activePluginId
            );
        }

        providers.put(pluginId, provider);
    }

    public synchronized void unregister(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        if (providers.remove(pluginId) != null) {
            log.debug("Unregistered FX provider for plugin '{}'", pluginId);
        }
    }

    public synchronized Optional<String> getActiveProviderPluginId() {
        RegisteredProvider active = resolveActiveProviderLocked();
        return active == null ? Optional.empty() : Optional.of(active.pluginId());
    }

    public Optional<ExchangeRate> findRate(String fromCurrency, String toCurrency) {
        String from = normalizeCurrency(fromCurrency);
        String to = normalizeCurrency(toCurrency);
        if (from == null || to == null) {
            return Optional.empty();
        }
        if (from.equals(to)) {
            return Optional.of(new ExchangeRate(BigDecimal.ONE, IDENTITY_SOURCE, Instant.now()));
        }

        RegisteredProvider active = resolveActiveProvider();
        if (active == null) {
            return Optional.empty();
        }

        try {
            Optional<ExchangeRate> quote = active.provider().findRate(from, to);
            if (quote == null) {
                log.warn("FX provider '{}' returned null Optional for {} -> {}", active.pluginId(), from, to);
                return Optional.empty();
            }
            return quote.filter(this::isValidQuote);
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            log.error("FX provider '{}' failed looking up {} -> {}", active.pluginId(), from, to, e);
            return Optional.empty();
        }
    }

    private synchronized RegisteredProvider resolveActiveProvider() {
        return resolveActiveProviderLocked();
    }

    private RegisteredProvider resolveActiveProviderLocked() {
        if (!configuredProviderPluginId.isEmpty()) {
            ExchangeRateProvider configured = providers.get(configuredProviderPluginId);
            return configured == null ? null : new RegisteredProvider(configuredProviderPluginId, configured);
        }
        if (providers.isEmpty()) {
            return null;
        }
        Map.Entry<String, ExchangeRateProvider> first = providers.entrySet().iterator().next();
        return new RegisteredProvider(first.getKey(), first.getValue());
    }

    private boolean isValidQuote(ExchangeRate quote) {
        return quote != null
                && quote.rate() != null
                && quote.rate().signum() > 0
                && quote.source() != null
                && !quote.source().isBlank()
                && quote.fetchedAt() != null;
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private record RegisteredProvider(String pluginId, ExchangeRateProvider provider) {
    }
}
