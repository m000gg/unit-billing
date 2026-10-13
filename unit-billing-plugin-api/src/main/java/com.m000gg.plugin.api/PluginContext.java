package com.m000gg.plugin.api;

import com.m000gg.plugin.api.event.BalanceChanged;

import java.util.function.Consumer;

public interface PluginContext{
    void onBalanceChanged(Consumer<BalanceChanged> handler);
}
