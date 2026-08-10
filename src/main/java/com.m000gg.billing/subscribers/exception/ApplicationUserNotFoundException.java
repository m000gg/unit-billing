package com.m000gg.billing.subscribers.exception;

import java.util.UUID;

public class ApplicationUserNotFoundException extends RuntimeException {
    public ApplicationUserNotFoundException(UUID id) {
        super("Application user not found: " + id);
    }
}
