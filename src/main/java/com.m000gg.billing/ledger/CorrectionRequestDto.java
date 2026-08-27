package com.m000gg.billing.ledger;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CorrectionRequestDto {
    @NotNull(message = "{ledger.validation.amount.required}")
    @Positive(message = "{ledger.validation.amount.positive}")
    @Digits(integer = 15, fraction = 4, message = "{ledger.validation.amount.digits}")
    private BigDecimal amount;

    @NotNull(message = "{ledger.validation.direction.required}")
    private CorrectionDirection direction;

    @NotBlank(message = "{ledger.validation.description.required}")
    @Size(max = 255, message = "{ledger.validation.description.size}")
    private String description;

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
}
