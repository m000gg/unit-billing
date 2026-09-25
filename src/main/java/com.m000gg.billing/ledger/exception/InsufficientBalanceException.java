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

package com.m000gg.billing.ledger.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;
    private final UUID userId;

    public InsufficientBalanceException(UUID userId, BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient balance for user %s: requested %s, available %s",
                userId, requested, available));
        this.messageKey = "errors.ledger.insufficientBalance";
        this.args = new Object[]{userId, requested, available};
        this.userId = userId;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }

    public UUID getUserId() {
        return userId;
    }
}
