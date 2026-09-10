package com.m000gg.billing.settings;

import jakarta.validation.constraints.NotBlank;

public class BillingSetupDto {
    @NotBlank(message = "{admin.billing-setup.baseCurrency.error}")
    private String baseCurrency;

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }
}
