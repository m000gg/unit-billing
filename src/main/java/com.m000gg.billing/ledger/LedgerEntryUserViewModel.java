package com.m000gg.billing.ledger;

import java.math.BigDecimal;
import java.time.Instant;

public class LedgerEntryUserViewModel {
    private BigDecimal amount;
    private String description;
    private Instant createdAt;
    private EntryType type;

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
