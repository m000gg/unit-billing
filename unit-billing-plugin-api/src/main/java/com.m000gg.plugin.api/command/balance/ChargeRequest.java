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
 * A request to charge a subscriber's balance.
 *
 * @param subscriberId   the subscriber to charge
 * @param amount         the amount in the base currency; must be greater than zero
 * @param idempotencyKey identifies this charge. It must be deterministic: build it from
 *                       the business identity of the charge, not from the current time or
 *                       a random value, so that repeating the same charge produces the
 *                       same key. It starts with the plugin id, for example
 *                       {@code subscriptions:<subscriptionId>:<periodNo>}. It must not be
 *                       blank and is at most 255 characters long. Reusing a key with a
 *                       different subscriber or amount is a contract violation.
 * @param description    human-readable text stored in the ledger entry and shown to
 *                       the subscriber (for example in balance-change notifications)
 */
public record ChargeRequest(UUID subscriberId,
                            BigDecimal amount,
                            String idempotencyKey,
                            String description) {

    private static final int MAX_KEY_LENGTH = 255;

    public ChargeRequest {
        Objects.requireNonNull(subscriberId, "subscriberId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(description, "description");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "idempotencyKey must not be blank and must be at most " + MAX_KEY_LENGTH + " characters");
        }
    }
}
