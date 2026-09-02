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
