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
import com.m000gg.plugin.api.query.fx.ExchangeRateDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Core implementation of {@link ExchangeRateDirectory} backed by configured platform
 * settings and {@link PluginExchangeRateRegistry}.
 */
@Component
@RequiredArgsConstructor
public class DefaultExchangeRateDirectory implements ExchangeRateDirectory {

    private final SystemSettingService systemSettingService;
    private final PluginExchangeRateRegistry exchangeRateRegistry;

    @Override
    public String baseCurrency() {
        return systemSettingService.getBaseCurrency();
    }

    @Override
    public Optional<ExchangeRate> findRate(String fromCurrency, String toCurrency) {
        return exchangeRateRegistry.findRate(fromCurrency, toCurrency);
    }
}
