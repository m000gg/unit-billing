package com.m000gg.billing.subscribers.exception;

public class EmailAlreadyTakenException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    public EmailAlreadyTakenException(String email) {
        super("Email already taken: " + email);
        this.messageKey = "errors.identity.emailAlreadyTaken";
        this.args = new Object[]{email};
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
