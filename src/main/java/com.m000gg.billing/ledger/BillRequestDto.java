package com.m000gg.billing.ledger;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class BillRequestDto {
    @NotNull(message = "{ledger.validation.amount.required}")
    @Positive(message = "{ledger.validation.amount.positive}")
    @Digits(integer = 15, fraction = 4, message = "{ledger.validation.amount.digits}")
    private BigDecimal amount;
    private String description;
    private String userCurrency;
    private String baseCurrency;
    private String exchangeRateSource;
    @NotNull(message = "{ledger.validation.userAmount.required}")
    @Positive(message = "{ledger.validation.userAmount.positive}")
    @Digits(integer = 15, fraction = 4, message = "{ledger.validation.userAmount.digits}")
    private BigDecimal amountInBaseCurrency;
    @NotNull(message = "{ledger.validation.exchangeRate.required}")
    @Positive(message = "{ledger.validation.exchangeRate.positive}")
    private BigDecimal exchangeRate;

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserCurrency() {
        return userCurrency;
    }
    public void setUserCurrency(String userCurrency) {
        this.userCurrency = userCurrency;
    }
    public String getBaseCurrency() {
        return baseCurrency;
    }
    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }
    public String getExchangeRateSource() {
        return exchangeRateSource;
    }
    public void setExchangeRateSource(String exchangeRateSource) {
        this.exchangeRateSource = exchangeRateSource;
    }
    public BigDecimal getAmountInBaseCurrency() {
        return amountInBaseCurrency;
    }
    public void setAmountInBaseCurrency(BigDecimal amountInBaseCurrency) {
        this.amountInBaseCurrency = amountInBaseCurrency;
    }
    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }
    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
}
