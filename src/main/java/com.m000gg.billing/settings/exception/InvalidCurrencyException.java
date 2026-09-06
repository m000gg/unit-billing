package com.m000gg.billing.settings.exception;


public class InvalidCurrencyException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;
    private final String invalidCurrencyCode;

    public InvalidCurrencyException(String invalidCurrencyCode) {
        super(String.format("Invalid or unsupported currency code: %s", invalidCurrencyCode));
        this.messageKey = "errors.validation.invalidCurrency";
        this.args = new Object[]{invalidCurrencyCode};
        this.invalidCurrencyCode = invalidCurrencyCode;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }

    public String getInvalidCurrencyCode() {
        return invalidCurrencyCode;
    }
}
