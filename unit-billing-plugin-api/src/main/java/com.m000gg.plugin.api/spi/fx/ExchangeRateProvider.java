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

package com.m000gg.plugin.api.spi.fx;

import com.m000gg.plugin.api.query.fx.ExchangeRate;

import java.util.Optional;

/**
 * Extension point implemented by external FX plugins to supply currency conversion rates.
 */
public interface ExchangeRateProvider {

    /**
     * Looks up a quote converting 1 unit of {@code fromCurrency} into {@code toCurrency}.
     * Implementations should serve rates from an in-memory cache rather than blocking callers
     * on synchronous network I/O.
     *
     * @param fromCurrency source currency ISO 4217 code (upper-case)
     * @param toCurrency   target currency ISO 4217 code (upper-case)
     * @return an {@link ExchangeRate} quote if available, or empty if unsupported or stale
     */
    Optional<ExchangeRate> findRate(String fromCurrency, String toCurrency);
}
