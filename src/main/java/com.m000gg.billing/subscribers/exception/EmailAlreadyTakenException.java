package com.m000gg.billing.subscribers.exception;

public class EmailAlreadyTakenException extends RuntimeException {
    public EmailAlreadyTakenException(){
        super();
    }

    public EmailAlreadyTakenException(String message) {
        super(message);
    }

    public EmailAlreadyTakenException(String message, Throwable cause){
        super(message, cause);
    }
}
