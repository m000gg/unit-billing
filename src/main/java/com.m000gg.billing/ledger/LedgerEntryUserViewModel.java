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
import java.time.Instant;

public class LedgerEntryUserViewModel {
    private BigDecimal amount;
    private String description;
    private Instant createdAt;
    private EntryType type;
    private EntrySource source;
    private String userCurrency;
    public String getUserCurrency() {
        return userCurrency;
    }
    public void setUserCurrency(String userCurrency) {
        this.userCurrency = userCurrency;
    }
    public EntrySource getSource() { return source; }
    public void setSource(EntrySource source) { this.source = source; }
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
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public EntryType getType() {
        return type;
    }
    public void setType(EntryType type) {
        this.type = type;
    }
}
