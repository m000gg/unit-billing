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
    public void listenBalanceChanged(BalanceChanged event){
        log.info("Balance changed: {}", event);
        for (Consumer<BalanceChanged> handler: pluginManager.balanceChangedHandlers()){
            try{
                handler.accept(event);
            } catch (Exception | LinkageError e) {
                log.error("Plugin handler failed", e);
            }

        }

    }
}
