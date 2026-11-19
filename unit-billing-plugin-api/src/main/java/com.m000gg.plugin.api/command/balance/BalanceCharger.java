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

public interface BalanceCharger {

    /**
     * Charges the subscriber's balance by the requested amount.
     * <p>
     * The call is idempotent: repeating it with the same idempotency key returns
     * {@link ChargeStatus#SUCCESS} with the id of the entry created by the first call.
     * The balance is not deducted again and no second balance-change event is published.
     * <p>
     * If the balance is too low, {@link ChargeStatus#INSUFFICIENT_FUNDS} is returned,
     * no ledger entry is created and the balance stays unchanged. A later call with
     * the same key tries to charge again.
     * <p>
     * The call is thread-safe: concurrent charges of one subscriber are processed
     * one after another. The charge runs in its own transaction and is committed
     * when this method returns.
     *
     * @param request the charge to perform
     * @return the outcome; the ledger entry id is set only for {@code SUCCESS}
     * @throws IllegalArgumentException if the amount is not positive, the subscriber
     *         does not exist, the subscriber's currency differs from the base currency,
     *         or the key was already used with a different subscriber or amount
     */
    ChargeResult charge(ChargeRequest request);
}
