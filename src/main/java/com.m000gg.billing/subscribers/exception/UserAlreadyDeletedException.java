package com.m000gg.billing.subscribers.exception;

public class UserAlreadyDeletedException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    public UserAlreadyDeletedException() {
        super();
        this.messageKey = "errors.subscribers.userAlreadyDeleted";
        this.args = new Object[0];
    }

    public UserAlreadyDeletedException(String message) {
        super(message);
        this.messageKey = "errors.subscribers.userAlreadyDeleted";
        this.args = new Object[0];
    }

    public UserAlreadyDeletedException(String message, Throwable cause) {
        super(message, cause);
        this.messageKey = "errors.subscribers.userAlreadyDeleted";
        this.args = new Object[0];
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
