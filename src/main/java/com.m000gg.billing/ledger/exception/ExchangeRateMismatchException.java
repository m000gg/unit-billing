package com.m000gg.billing.ledger.exception;

import java.math.BigDecimal;

public class ExchangeRateMismatchException extends RuntimeException {

  private final String messageKey;
  private final Object[] args;

  public ExchangeRateMismatchException(BigDecimal providedAmount, BigDecimal baseAmount,
                                       BigDecimal rate, BigDecimal expectedAmount) {
    super(String.format("Provided amount %s does not match baseAmount %s at rate %s (expected %s)",
            providedAmount, baseAmount, rate, expectedAmount));
    this.messageKey = "errors.ledger.exchangeRateMismatch";
    this.args = new Object[]{providedAmount, baseAmount, rate, expectedAmount};
  }

  public String getMessageKey() {
    return messageKey;
  }

  public Object[] getArgs() {
    return args;
  }
}
