package com.m000gg.billing.ledger;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CorrectionRequestDto {
    @NotNull
    @Positive
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 integer and 4 fractional digits")
    private BigDecimal amount;

    @NotNull(message = "Select a direction")
    private CorrectionDirection direction;

    @NotBlank(message = "Reason is required")
    @Size(max = 255)
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
