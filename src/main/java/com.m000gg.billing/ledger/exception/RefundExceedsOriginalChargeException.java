package com.m000gg.billing.ledger.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class RefundExceedsOriginalChargeException extends RuntimeException {

    public RefundExceedsOriginalChargeException(UUID originalEntryId, BigDecimal requested, BigDecimal originalAmount) {
        super(String.format("Refund amount %s exceeds original charge %s amount (%s)",
                requested, originalEntryId, originalAmount));
    }
}
