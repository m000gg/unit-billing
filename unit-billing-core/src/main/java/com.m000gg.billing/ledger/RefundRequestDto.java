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

package com.m000gg.billing.ledger;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class RefundRequestDto {
    @NotNull(message = "{ledger.validation.originalEntryId.required}")
    private UUID originalEntryId;

    @Positive(message = "{ledger.validation.amount.positive}")
    @NotNull(message = "{ledger.validation.amount.required}")
    @Digits(integer = 15, fraction = 2, message = "{ledger.validation.amount.digits}")
    private BigDecimal amount;
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

    public UUID getOriginalEntryId() {
        return originalEntryId;
    }

    public void setOriginalEntryId(UUID originalEntryId) {
        this.originalEntryId = originalEntryId;
    }

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
