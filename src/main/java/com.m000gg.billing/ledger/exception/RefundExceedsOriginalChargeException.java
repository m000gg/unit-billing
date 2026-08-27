package com.m000gg.billing.ledger.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class RefundExceedsOriginalChargeException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    public RefundExceedsOriginalChargeException(UUID originalEntryId, BigDecimal requested, BigDecimal originalAmount) {
        super(String.format("Refund amount %s exceeds original charge %s amount (%s)",
                requested, originalEntryId, originalAmount));
        this.messageKey = "errors.ledger.refundExceedsOriginalCharge";
        this.args = new Object[]{originalEntryId, requested, originalAmount};
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
