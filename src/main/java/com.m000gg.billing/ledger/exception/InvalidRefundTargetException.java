package com.m000gg.billing.ledger.exception;

import java.util.UUID;

public class InvalidRefundTargetException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    public InvalidRefundTargetException(UUID originalEntryId) {
        super("Invalid refund target: ledger entry " + originalEntryId
                + " does not exist, does not belong to this subscriber, is not a charge, or has already been refunded");
        this.messageKey = "errors.ledger.invalidRefundTarget";
        this.args = new Object[]{originalEntryId};
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
