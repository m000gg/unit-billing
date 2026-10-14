package com.m000gg.billing.plugin;

import com.m000gg.billing.plugin.context.DefaultPluginContext;

import java.nio.file.Path;

public record RunningPlugin(String id, Path pathToJar, PluginLoader.LoadedPlugin loadedPlugin, DefaultPluginContext defaultPluginContext) {
}
