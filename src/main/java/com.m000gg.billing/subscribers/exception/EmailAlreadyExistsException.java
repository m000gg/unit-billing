package com.m000gg.billing.subscribers.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(){
        super();
    }

    public EmailAlreadyExistsException(String message){
        super(message);
    }

    public EmailAlreadyExistsException(String message, Throwable cause){
        super(message, cause);
    }
}
