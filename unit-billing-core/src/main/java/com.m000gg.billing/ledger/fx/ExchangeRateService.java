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

package com.m000gg.billing.ledger.fx;

import com.m000gg.billing.settings.SystemSettingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExchangeRateService {

    private final Map<String, ExchangeRateProvider> providersById;
    private final List<ExchangeRateProvider> providers;
    private final SystemSettingService systemSettingService;

    public ExchangeRateService(List<ExchangeRateProvider> providers,
                               SystemSettingService systemSettingService) {
        this.providers = List.copyOf(providers);
        this.providersById = providers.stream()
                .collect(Collectors.toMap(ExchangeRateProvider::id, Function.identity()));
        this.systemSettingService = systemSettingService;
    }

    public Optional<ExchangeRateQuote> getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            return Optional.empty();
        }
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return Optional.of(new ExchangeRateQuote(BigDecimal.ONE.setScale(6), "identity", Instant.now()));
        }
        String activeId = systemSettingService.getExchangeRateProviderId();
        ExchangeRateProvider provider = providersById.get(activeId);
        return provider == null ? Optional.empty() : provider.findRate(fromCurrency, toCurrency);
    }

    public List<ExchangeRateProvider> getProviders() {
        return providers;
    }
}
