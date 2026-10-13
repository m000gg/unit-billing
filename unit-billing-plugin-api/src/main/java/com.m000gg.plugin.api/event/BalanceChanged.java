package com.m000gg.plugin.api.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BalanceChanged(UUID ledgerId,
                             UUID userId,
                             BalanceChangeType balanceChangeType,
                             BalanceChangeDirection balanceChangeDirection,
                             BigDecimal amountInUserCurrency,
                             String userCurrency,
                             BigDecimal balanceAfterTransaction,
                             Instant transactionTime) {
}
