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

package com.m000gg.billing.settings;


import com.m000gg.billing.settings.exception.InvalidCurrencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemSettingService {
    @Autowired
    private SystemSettingRepository systemSettingRepository;

    private static final String BASE_CURRENCY_KEY = "BASE_CURRENCY";

    private static final Set<String> VALID_CURRENCIES = Currency.getAvailableCurrencies()
            .stream()
            .map(Currency::getCurrencyCode)
            .collect(Collectors.toSet());

    @Cacheable("baseCurrencyConfigured")
    public boolean isBaseCurrencyConfigured() {
        return systemSettingRepository.existsById(BASE_CURRENCY_KEY);
    }

    @Cacheable("baseCurrency")
    public String getBaseCurrency() {
        String baseCurrency = systemSettingRepository.findValueByKey(BASE_CURRENCY_KEY).orElseThrow(() -> new IllegalStateException("Required system setting not found!"));
        return baseCurrency;
    }

    @CacheEvict(value = {"baseCurrency", "baseCurrencyConfigured"}, allEntries = true)
    public void saveInitialSetup(BillingSetupDto billingSetupDto) {

        if (isBaseCurrencyConfigured()) {
            throw new IllegalStateException("Base currency is already configured and cannot be changed.");
        }

        saveSingleSetting(BASE_CURRENCY_KEY, billingSetupDto.getBaseCurrency(), "Base currency of platform.");
    }

    private void saveSingleSetting(String key, String value, String description) {
        if (value != null && key != null) {
            SystemSetting systemSetting = new SystemSetting();
            systemSetting.setKey(key);
            if (value.isBlank() || !VALID_CURRENCIES.contains(value.toUpperCase())) {
                throw new InvalidCurrencyException(value);
            }
            systemSetting.setSettingValue(value.toUpperCase());

            systemSetting.setDescription(description);
            systemSetting.setUpdatedAt(Instant.now());
            systemSettingRepository.save(systemSetting);
        }
    }
}
