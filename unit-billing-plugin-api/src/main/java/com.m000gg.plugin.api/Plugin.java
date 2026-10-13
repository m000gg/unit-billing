package com.m000gg.plugin.api;

public interface Plugin {
    void onEnable(PluginContext pluginContext);
    void onDisable();
    String getId();
}
