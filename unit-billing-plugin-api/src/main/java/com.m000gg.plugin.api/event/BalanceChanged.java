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
