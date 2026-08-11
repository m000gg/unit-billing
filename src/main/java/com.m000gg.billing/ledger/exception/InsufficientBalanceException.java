package com.m000gg.billing.ledger.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(UUID userId, BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient balance for user %s: requested %s, available %s",
                userId, requested, available));
    }
}
