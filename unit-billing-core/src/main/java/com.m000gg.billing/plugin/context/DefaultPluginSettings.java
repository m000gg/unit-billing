package com.m000gg.billing.plugin.context;

import com.m000gg.plugin.api.query.settings.PluginSettings;

import java.util.Map;
import java.util.Optional;

public class DefaultPluginSettings implements PluginSettings {

    private final Map<String, String> values;

    public DefaultPluginSettings(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key));
    }
}
