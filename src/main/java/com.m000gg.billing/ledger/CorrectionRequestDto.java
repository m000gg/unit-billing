package com.m000gg.billing.ledger;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CorrectionRequestDto {
    @NotNull(message = "{ledger.validation.amount.required}")
    @Positive(message = "{ledger.validation.amount.positive}")
    @Digits(integer = 15, fraction = 2, message = "{ledger.validation.amount.digits}")
    private BigDecimal amount;

    @NotNull(message = "{ledger.validation.direction.required}")
    private CorrectionDirection direction;

    @NotBlank(message = "{ledger.validation.description.required}")
    @Size(max = 255, message = "{ledger.validation.description.size}")
    private String description;
    private String userCurrency;
    private String baseCurrency;
    @NotNull(message = "{ledger.validation.exchangeRate.required}")
    @Positive(message = "{ledger.validation.exchangeRate.positive}")
    private BigDecimal exchangeRate;
    private String exchangeRateSource;
    @NotNull(message = "{ledger.validation.amount.required}")
    @Positive(message = "{ledger.validation.amount.positive}")
    @Digits(integer = 15, fraction = 2, message = "{ledger.validation.amount.digits}")
    private BigDecimal amountInBaseCurrency;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public CorrectionDirection getDirection() {
        return direction;
    }

    public void setDirection(CorrectionDirection direction) {
        this.direction = direction;
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

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
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
}
