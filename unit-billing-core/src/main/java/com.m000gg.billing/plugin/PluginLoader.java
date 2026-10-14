/*
 * Copyright 2026 Vladyslav Livandovskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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