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

    public boolean isBaseCurrencyConfigured() {
        return systemSettingRepository.existsById(BASE_CURRENCY_KEY);
    }

    @Cacheable("baseCurrency")
    public String getBaseCurrency(){
        String baseCurrency = systemSettingRepository.findValueByKey(BASE_CURRENCY_KEY).orElseThrow(() -> new IllegalStateException("Required system setting not found!"));
        return baseCurrency;
    }

    @CacheEvict(value = "baseCurrency", allEntries = true)
    public void saveInitialSetup(BillingSetupDto billingSetupDto){
        saveSingleSetting(BASE_CURRENCY_KEY ,billingSetupDto.getBaseCurrency(), "Base currency of platform.");
    }

    private void saveSingleSetting(String key, String value, String description) {
        if (value != null && key != null){
            SystemSetting systemSetting = new SystemSetting();
            systemSetting.setKey(key);
            if ( value.isBlank() || !VALID_CURRENCIES.contains(value.toUpperCase())) {
                throw new InvalidCurrencyException(value);
            }
            systemSetting.setSettingValue(value.toUpperCase());

            systemSetting.setDescription(description);
            systemSetting.setUpdatedAt(Instant.now());
            systemSettingRepository.save(systemSetting);
        }
    }
}
