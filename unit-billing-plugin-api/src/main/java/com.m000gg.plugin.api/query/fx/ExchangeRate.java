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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable exchange rate quote returned by {@link ExchangeRateDirectory}
 * and supplied by {@link com.m000gg.plugin.api.spi.fx.ExchangeRateProvider}.
 *
 * @param rate      how many units of {@code toCurrency} correspond to 1 unit of {@code fromCurrency}
 * @param source    identifier of the provider that supplied the quote
 * @param fetchedAt timestamp when the rate was fetched
 */
public record ExchangeRate(BigDecimal rate, String source, Instant fetchedAt) {

    public ExchangeRate {
        Objects.requireNonNull(rate, "rate must not be null");
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
        Objects.requireNonNull(source, "source must not be null");
        if (source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
    }
}
