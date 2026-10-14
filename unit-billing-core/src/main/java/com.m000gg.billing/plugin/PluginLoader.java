package com.m000gg.billing.plugin;

import com.m000gg.plugin.api.Plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;

public class PluginLoader {

    public record LoadedPlugin(Plugin plugin, URLClassLoader classLoader) {
    }

    public LoadedPlugin load(Path jarPath) throws IOException {
        if (!Files.isRegularFile(jarPath)) {
            throw new IllegalArgumentException("Plugin jar not found: " + jarPath);
        }

        URL url = jarPath.toUri().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, Plugin.class.getClassLoader());

        try {
            Plugin plugin = ServiceLoader.load(Plugin.class, classLoader)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No Plugin implementation found in " + jarPath));
            return new LoadedPlugin(plugin, classLoader);
        } catch (RuntimeException | Error e) {
            classLoader.close();
            throw e;
        }
    }
}
