package com.m000gg.billing.ledger.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;
    private final UUID userId;

    public InsufficientBalanceException(UUID userId, BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient balance for user %s: requested %s, available %s",
                userId, requested, available));
        this.messageKey = "errors.ledger.insufficientBalance";
        this.args = new Object[]{userId, requested, available};
        this.userId = userId;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }

    public UUID getUserId() {
        return userId;
    }
}
