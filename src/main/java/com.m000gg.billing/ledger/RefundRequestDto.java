package com.m000gg.billing.ledger;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class RefundRequestDto {
    @NotNull
    private UUID originalEntryId;

    @Positive
    @NotNull
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 integer and 4 fractional digits")
    private BigDecimal amount;

    private String description;

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
}
