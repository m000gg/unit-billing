package com.m000gg.plugin.api;

import com.m000gg.plugin.api.event.BalanceChanged;
import com.m000gg.plugin.api.query.settings.PluginSettings;
import com.m000gg.plugin.api.query.user.UserDirectory;

import java.util.function.Consumer;

public interface PluginContext{
    void onBalanceChanged(Consumer<BalanceChanged> handler);
    UserDirectory users();
    PluginSettings settings();
}
