package com.m000gg.billing.subscribers.exception;

public class UserAlreadyDeletedException extends RuntimeException {
    public UserAlreadyDeletedException() {super();}

    public UserAlreadyDeletedException(String message) {
        super(message);
    }

    public UserAlreadyDeletedException(String message, Throwable cause){
        super(message, cause);
    }
}
