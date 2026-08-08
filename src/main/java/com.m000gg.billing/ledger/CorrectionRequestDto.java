package com.m000gg.billing.ledger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CorrectionRequestDto {
    @NotNull
    @Positive
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
