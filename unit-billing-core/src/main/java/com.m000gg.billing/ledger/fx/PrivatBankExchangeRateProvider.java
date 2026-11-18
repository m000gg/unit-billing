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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PrivatBankExchangeRateProvider implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(PrivatBankExchangeRateProvider.class);
    private static final String API_URL = "https://api.privatbank.ua/p24api/pubinfo?exchange&coursid=11";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final int RATE_SCALE = 6;

    private final RestClient restClient;
    private volatile CachedRates cache;

    public PrivatBankExchangeRateProvider() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String id() {
        return "privatbank";
    }

    @Override
    public String displayName() {
        return "PrivatBank";
    }

    @Override
    public Optional<ExchangeRateQuote> findRate(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            return Optional.empty();
        }
        String from = fromCurrency.trim().toUpperCase();
        String to = toCurrency.trim().toUpperCase();
        if (from.equals(to)) {
            return Optional.of(new ExchangeRateQuote(BigDecimal.ONE.setScale(RATE_SCALE), id(), Instant.now()));
        }

        CachedRates rates = getOrFetchRates();
        if (rates == null) {
            return Optional.empty();
        }

        BigDecimal rate = rates.rates().get(new CurrencyPair(from, to));
        return rate == null
                ? Optional.empty()
                : Optional.of(new ExchangeRateQuote(rate, id(), rates.fetchedAt()));
    }

    private CachedRates getOrFetchRates() {
        Instant now = Instant.now();
        CachedRates current = this.cache;
        if (current != null && now.isBefore(current.expiresAt())) {
            return current;
        }
        synchronized (this) {
            current = this.cache;
            if (current != null && now.isBefore(current.expiresAt())) {
                return current;
            }
            try {
                PrivatRateDto[] response = restClient.get()
                        .uri(API_URL)
                        .retrieve()
                        .body(PrivatRateDto[].class);
                if (response == null) {
                    return current;
                }
                Map<CurrencyPair, BigDecimal> map = new HashMap<>();
                for (PrivatRateDto item : response) {
                    if (item.ccy() != null && item.base_ccy() != null && item.sale() != null) {
                        String ccy = item.ccy().trim().toUpperCase();
                        String baseCcy = item.base_ccy().trim().toUpperCase();
                        BigDecimal sale = new BigDecimal(item.sale().trim()).setScale(RATE_SCALE, RoundingMode.HALF_UP);

                        map.put(new CurrencyPair(ccy, baseCcy), sale);
                        if (sale.signum() > 0) {
                            map.put(new CurrencyPair(baseCcy, ccy), BigDecimal.ONE.divide(sale, RATE_SCALE, RoundingMode.HALF_UP));
                        }
                    }
                }
                CachedRates fresh = new CachedRates(Map.copyOf(map), now, now.plus(CACHE_TTL));
                this.cache = fresh;
                return fresh;
            } catch (Exception ex) {
                log.warn("Failed to fetch exchange rates from PrivatBank", ex);
                return current;
            }
        }
    }

    private record CurrencyPair(String from, String to) {}

    private record PrivatRateDto(String ccy, String base_ccy, String buy, String sale) {}

    private record CachedRates(Map<CurrencyPair, BigDecimal> rates, Instant fetchedAt, Instant expiresAt) {}
}
