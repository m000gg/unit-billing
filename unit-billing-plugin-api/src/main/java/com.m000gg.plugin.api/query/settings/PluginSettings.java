package com.m000gg.plugin.api.query.settings;

import java.util.Optional;

public interface PluginSettings {
    Optional<String> get(String key);
}
