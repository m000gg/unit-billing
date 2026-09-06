package com.m000gg.billing.settings;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SystemSettingService {
    @Autowired
    private SystemSettingRepository systemSettingRepository;

    private static final String BASE_CURRENCY_KEY = "BASE_CURRENCY";

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
            systemSetting.setSettingValue(value);
            systemSetting.setDescription(description);
            systemSetting.setUpdatedAt(Instant.now());
            systemSettingRepository.save(systemSetting);
        }
    }
}
