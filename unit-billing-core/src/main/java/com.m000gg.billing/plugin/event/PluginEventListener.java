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

package com.m000gg.billing.plugin.event;

import com.m000gg.billing.plugin.PluginManager;
import com.m000gg.plugin.api.event.BalanceChanged;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class PluginEventListener {
    private final PluginManager pluginManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void listenBalanceChanged(BalanceChanged event) {
        log.info("Balance changed: {}", event);
        for (Consumer<BalanceChanged> handler : pluginManager.balanceChangedHandlers()) {
            try {
                handler.accept(event);
            } catch (Exception | LinkageError e) {
                log.error("Plugin handler failed", e);
            }

        }

    }
}