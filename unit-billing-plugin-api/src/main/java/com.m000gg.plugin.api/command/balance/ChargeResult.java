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

import java.util.Objects;
import java.util.UUID;

/**
 * Result of {@link BalanceCharger#charge(ChargeRequest)}.
 *
 * @param status        the outcome of the charge
 * @param ledgerEntryId id of the ledger entry created by the charge; {@code null}
 *                      when the status is {@link ChargeStatus#INSUFFICIENT_FUNDS}
 */
public record ChargeResult(ChargeStatus status, UUID ledgerEntryId) {

    public ChargeResult {
        Objects.requireNonNull(status, "status");
        if (status == ChargeStatus.SUCCESS && ledgerEntryId == null) {
            throw new IllegalArgumentException("ledgerEntryId is required for SUCCESS");
        }
    }
}