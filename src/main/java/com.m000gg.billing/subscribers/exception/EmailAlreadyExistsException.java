package com.m000gg.billing.subscribers.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email);
        this.messageKey = "errors.identity.emailAlreadyExists";
        this.args = new Object[]{email};
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
