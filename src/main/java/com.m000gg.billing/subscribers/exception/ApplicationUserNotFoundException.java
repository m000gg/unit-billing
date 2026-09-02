package com.m000gg.billing.subscribers.exception;

import java.util.UUID;

public class ApplicationUserNotFoundException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    public ApplicationUserNotFoundException(UUID id) {
        super("Application user not found: " + id);
        this.messageKey = "errors.subscribers.notFound";
        this.args = new Object[]{id};
    }

    public String getMessageKey() {
        return messageKey;
    }
    public Object[] getArgs() {
        return args;
    }
}
