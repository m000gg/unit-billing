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

import com.m000gg.billing.settings.SystemSettingService;
import com.m000gg.plugin.api.query.fx.ExchangeRate;
import com.m000gg.plugin.api.spi.fx.ExchangeRateProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PluginExchangeRateRegistryTest {

    @Test
    void returnsIdentityRateWithoutAnyRegisteredPlugin() {
        PluginExchangeRateRegistry registry = new PluginExchangeRateRegistry();

        Optional<ExchangeRate> rate = registry.findRate(" usd ", "USD");

        assertThat(rate).isPresent();
        assertThat(rate.get().rate()).isEqualByComparingTo("1");
        assertThat(rate.get().source()).isEqualTo(PluginExchangeRateRegistry.IDENTITY_SOURCE);
    }

    @Test
    void returnsEmptyForInvalidInputsOrWhenNoProviderRegistered() {
        PluginExchangeRateRegistry registry = new PluginExchangeRateRegistry();

        assertThat(registry.findRate(null, "EUR")).isEmpty();
        assertThat(registry.findRate("USD", "  ")).isEmpty();
        assertThat(registry.findRate("USD", "EUR")).isEmpty();
        assertThat(registry.getActiveProviderPluginId()).isEmpty();
    }

    @Test
    void delegatesNormalizedCurrenciesToRegisteredProviderAndUnregistersCleanly() {
        PluginExchangeRateRegistry registry = new PluginExchangeRateRegistry();
        Instant now = Instant.parse("2026-11-19T20:00:00Z");

        registry.register("nbu-fx", (from, to) -> {
            assertThat(from).isEqualTo("GBP");
            assertThat(to).isEqualTo("EUR");
            return Optional.of(new ExchangeRate(new BigDecimal("1.170000"), "nbu", now));
        });

        assertThat(registry.getActiveProviderPluginId()).contains("nbu-fx");
        assertThat(registry.findRate("gbp", "eur"))
                .contains(new ExchangeRate(new BigDecimal("1.170000"), "nbu", now));

        registry.unregister("nbu-fx");

        assertThat(registry.getActiveProviderPluginId()).isEmpty();
        assertThat(registry.findRate("GBP", "EUR")).isEmpty();
    }

    @Test
    void isolatesExceptionsLinkageErrorsAndNullReturnFromFaultyProvider() {
        PluginExchangeRateRegistry registry = new PluginExchangeRateRegistry();

        registry.register("throwing-fx", (from, to) -> {
            throw new IllegalStateException("upstream down");
        });
        assertThat(registry.findRate("USD", "EUR")).isEmpty();

        registry.register("linkage-fx", (from, to) -> {
            throw new NoClassDefFoundError("missing dep");
        });
        registry.unregister("throwing-fx");
        assertThat(registry.findRate("USD", "EUR")).isEmpty();

        registry.register("null-fx", (from, to) -> null);
        registry.unregister("linkage-fx");
        assertThat(registry.findRate("USD", "EUR")).isEmpty();
    }

    @Test
    void respectsConfiguredProviderOrFirstRegisteredDeterministically() {
        PluginExchangeRateRegistry firstWins = new PluginExchangeRateRegistry();
        Instant now = Instant.now();
        firstWins.register("first-fx", (from, to) -> Optional.of(new ExchangeRate(BigDecimal.TWO, "first", now)));
        firstWins.register("second-fx", (from, to) -> Optional.of(new ExchangeRate(BigDecimal.TEN, "second", now)));

        assertThat(firstWins.getActiveProviderPluginId()).contains("first-fx");
        assertThat(firstWins.findRate("USD", "UAH")).get().extracting(ExchangeRate::source).isEqualTo("first");

        firstWins.unregister("first-fx");
        assertThat(firstWins.getActiveProviderPluginId()).contains("second-fx");
        assertThat(firstWins.findRate("USD", "UAH")).get().extracting(ExchangeRate::source).isEqualTo("second");

        PluginExchangeRateRegistry pinned = new PluginExchangeRateRegistry("second-fx");
        pinned.register("first-fx", (from, to) -> Optional.of(new ExchangeRate(BigDecimal.TWO, "first", now)));
        assertThat(pinned.getActiveProviderPluginId()).isEmpty();
        assertThat(pinned.findRate("USD", "UAH")).isEmpty();

        pinned.register("second-fx", (from, to) -> Optional.of(new ExchangeRate(BigDecimal.TEN, "second", now)));
        assertThat(pinned.getActiveProviderPluginId()).contains("second-fx");
        assertThat(pinned.findRate("USD", "UAH")).get().extracting(ExchangeRate::source).isEqualTo("second");
    }

    @Test
    void exchangeRateRecordAndDirectoryValidateInputsAndDelegate() {
        Instant now = Instant.now();
        assertThatNullPointerException().isThrownBy(() -> new ExchangeRate(null, "nbu", now));
        assertThatIllegalArgumentException().isThrownBy(() -> new ExchangeRate(BigDecimal.ZERO, "nbu", now));
        assertThatIllegalArgumentException().isThrownBy(() -> new ExchangeRate(new BigDecimal("-1"), "nbu", now));
        assertThatIllegalArgumentException().isThrownBy(() -> new ExchangeRate(BigDecimal.ONE, "  ", now));
        assertThatNullPointerException().isThrownBy(() -> new ExchangeRate(BigDecimal.ONE, "nbu", null));

        PluginExchangeRateRegistry registry = new PluginExchangeRateRegistry();
        registry.register("nbu-fx", (from, to) -> Optional.of(new ExchangeRate(new BigDecimal("41.50"), "nbu", now)));
        SystemSettingService settingsStub = new SystemSettingService() {
            @Override
            public String getBaseCurrency() {
                return "USD";
            }
        };

        DefaultExchangeRateDirectory directory = new DefaultExchangeRateDirectory(settingsStub, registry);
        assertThat(directory.baseCurrency()).isEqualTo("USD");
        assertThat(directory.findRate("USD", "UAH"))
                .contains(new ExchangeRate(new BigDecimal("41.50"), "nbu", now));
    }
}
