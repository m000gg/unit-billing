package com.m000gg.billing.plugin.context;

import com.m000gg.plugin.api.PluginContext;
import com.m000gg.plugin.api.event.BalanceChanged;
import com.m000gg.plugin.api.query.settings.PluginSettings;
import com.m000gg.plugin.api.query.user.UserDirectory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DefaultPluginContext implements PluginContext {

    private final List<Consumer<BalanceChanged>> balanceChangedHandlers = new CopyOnWriteArrayList<>();
    private final UserDirectory userDirectory;
    private final PluginSettings pluginSettings;

    public DefaultPluginContext(UserDirectory userDirectory, PluginSettings pluginSettings) {
        this.userDirectory = userDirectory;
        this.pluginSettings = pluginSettings;
    }

    @Override
    public void onBalanceChanged(Consumer<BalanceChanged> handler) {
        balanceChangedHandlers.add(handler);
    }

    @Override
    public UserDirectory users() {
        return userDirectory;
    }

    @Override
    public PluginSettings settings() {
        return pluginSettings;
    }

    public List<Consumer<BalanceChanged>> getBalanceChangedHandlers() {
        return List.copyOf(balanceChangedHandlers);
    }
}
