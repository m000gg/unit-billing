/*
 * Copyright 2026 Vladyslav Livandovskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m000gg.plugin.api.command.balance;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Result of {@link BalanceCharger#charge(ChargeRequest)}.
 *
 * @param status          the outcome of the charge
 * @param ledgerEntryId   id of the ledger entry created by the charge; {@code null}
 *                        when the status is {@link ChargeStatus#INSUFFICIENT_FUNDS}
 * @param chargedAmount   actual amount deducted in the subscriber's currency; {@code null}
 *                        when not charged or unavailable
 * @param chargedCurrency currency code of {@code chargedAmount}; {@code null}
 *                        when not charged or unavailable
 * @param exchangeRate    exchange rate applied from platform base currency to
 *                        {@code chargedCurrency}; {@code null} when not charged or unavailable
 */
public record ChargeResult(
        ChargeStatus status,
        UUID ledgerEntryId,
        BigDecimal chargedAmount,
        String chargedCurrency,
        BigDecimal exchangeRate
) {

    public ChargeResult {
        Objects.requireNonNull(status, "status");
        if (status == ChargeStatus.SUCCESS && ledgerEntryId == null) {
            throw new IllegalArgumentException("ledgerEntryId is required for SUCCESS");
        }
        if (status != ChargeStatus.SUCCESS
                && (ledgerEntryId != null || chargedAmount != null || chargedCurrency != null || exchangeRate != null)) {
            throw new IllegalArgumentException("charge details are allowed only for SUCCESS");
        }
        if (chargedAmount != null && chargedAmount.signum() <= 0) {
            throw new IllegalArgumentException("chargedAmount must be positive");
        }
        if (chargedCurrency != null && chargedCurrency.isBlank()) {
            throw new IllegalArgumentException("chargedCurrency must not be blank");
        }
        if (chargedAmount != null && chargedCurrency == null) {
            throw new IllegalArgumentException("chargedCurrency is required when chargedAmount is set");
        }
        if (exchangeRate != null && exchangeRate.signum() <= 0) {
            throw new IllegalArgumentException("exchangeRate must be positive");
        }
    }

    public ChargeResult(ChargeStatus status, UUID ledgerEntryId) {
        this(status, ledgerEntryId, null, null, null);
    }
}