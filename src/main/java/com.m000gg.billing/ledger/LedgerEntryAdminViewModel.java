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

import java.math.BigDecimal;
import java.util.UUID;

public class LedgerEntryAdminViewModel extends LedgerEntryUserViewModel {
    private UUID subscriberId;
    private UUID originalEntryId;
    private UUID id;
    private boolean refundable;
    private EntrySource source;
    private UUID performedByAdmin;
    private BigDecimal amountInBaseCurrency;
    private BigDecimal exchangeRate;
    private String exchangeRateSource;
    private String baseCurrency;

    public EntrySource getSource() {
        return source;
    }
    public void setSource(EntrySource source) {
        this.source = source;
    }
    public UUID getPerformedByAdmin() {
        return performedByAdmin;
    }
    public void setPerformedByAdmin(UUID performedByAdmin) {
        this.performedByAdmin = performedByAdmin;
    }
    public boolean isRefundable() {
        return refundable;
    }
    public void setRefundable(boolean refundable) {
        this.refundable = refundable;
    }
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public UUID getSubscriberId() {
        return subscriberId;
    }
    public void setSubscriberId(UUID subscriberId) {
        this.subscriberId = subscriberId;
    }
    public UUID getOriginalEntryId() {
        return originalEntryId;
    }
    public void setOriginalEntryId(UUID originalEntryId) {
        this.originalEntryId = originalEntryId;
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
    public String getExchangeRateSource() {
        return exchangeRateSource;
    }
    public void setExchangeRateSource(String exchangeRateSource) {
        this.exchangeRateSource = exchangeRateSource;
    }
    public String getBaseCurrency() {
        return baseCurrency;
    }
    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }
}
