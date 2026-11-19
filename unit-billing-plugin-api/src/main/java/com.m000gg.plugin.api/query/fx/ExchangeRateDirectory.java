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

package com.m000gg.plugin.api.query.fx;

import java.util.Optional;

/**
 * Read-only access to the platform's base currency and active exchange rate source.
 */
public interface ExchangeRateDirectory {

    /**
     * @return ISO 4217 code of the platform's base currency
     * @throws IllegalStateException if the platform base currency has not been configured yet
     */
    String baseCurrency();

    /**
     * Looks up the exchange rate between two currencies using the active FX provider.
     *
     * @param fromCurrency source currency ISO 4217 code
     * @param toCurrency   target currency ISO 4217 code
     * @return the rate quote if available, or empty if unsupported or unavailable
     */
    Optional<ExchangeRate> findRate(String fromCurrency, String toCurrency);
}
