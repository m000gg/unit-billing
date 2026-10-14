package com.m000gg.billing.plugin;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "unitbilling")
@Getter
@Setter
public class PluginProperties {
    private Map<String, Map<String, String>> plugins = new HashMap<>();
}
