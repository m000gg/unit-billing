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

package com.m000gg.billing.plugin.context;

import com.m000gg.billing.ledger.LedgerService;
import com.m000gg.billing.settings.SystemSettingService;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserRepository;
import com.m000gg.plugin.api.command.balance.BalanceCharger;
import com.m000gg.plugin.api.command.balance.ChargeRequest;
import com.m000gg.plugin.api.command.balance.ChargeResult;
import com.m000gg.plugin.api.query.fx.ExchangeRate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DefaultBalanceCharger implements BalanceCharger {

    private final LedgerService ledgerService;
    private final ApplicationUserRepository applicationUserRepository;
    private final SystemSettingService systemSettingService;
    private final PluginExchangeRateRegistry exchangeRateRegistry;

    @Override
    public ChargeResult charge(ChargeRequest request) {
        Optional<ChargeResult> existing = ledgerService.findExistingPluginCharge(request);
        if (existing.isPresent()) {
            return existing.get();
        }

        ApplicationUser user = applicationUserRepository.findById(request.subscriberId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown subscriber: " + request.subscriberId()));

        String userCurrency = user.getUserCurrency();
        String baseCurrency = systemSettingService.getBaseCurrency();

        ExchangeRate quote = exchangeRateRegistry.findRate(baseCurrency, userCurrency)
                .orElseThrow(() -> new IllegalStateException(
                        "Exchange rate unavailable for " + baseCurrency + " -> " + userCurrency));

        return ledgerService.chargeForPlugin(request, userCurrency, baseCurrency, quote.rate(), quote.source());
    }
}
