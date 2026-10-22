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

import com.m000gg.billing.plugin.context.DefaultPluginContext;
import com.m000gg.billing.plugin.context.DefaultPluginSettings;
import com.m000gg.plugin.api.Plugin;
import com.m000gg.plugin.api.event.BalanceChanged;
import com.m000gg.plugin.api.query.settings.PluginSettings;
import com.m000gg.plugin.api.query.user.UserDirectory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class PluginManager {

    private static final long WORKER_SHUTDOWN_TIMEOUT_SECONDS = 5;

    @Value("${unitbilling.configuration.plugins.directory}")
    private String pluginDirectory;

    @Value("${unitbilling.configuration.plugins.event-queue-capacity:256}")
    private int eventQueueCapacity;

    private final UserDirectory userDirectory;
    private final PluginProperties pluginProperties;
    private final PluginLoader pluginLoader = new PluginLoader();

    private final ConcurrentMap<String, RunningPlugin> runningPlugins =
            new ConcurrentHashMap<>();

    @PostConstruct
    void startPlugins() {
        if (pluginDirectory == null || pluginDirectory.isBlank()) {
            log.error("Plugin directory is not configured");
            return;
        }

        if (eventQueueCapacity < 1) {
            log.error("Plugin event queue capacity must be greater than zero");
            return;
        }

        File directory = new File(pluginDirectory);

        try {
            Files.createDirectories(directory.toPath());
        } catch (IOException e) {
            log.error("Could not create plugin directory {}", directory, e);
            return;
        }

        if (!directory.isDirectory()) {
            log.error("Plugin path is not a directory: {}", directory);
            return;
        }

        File[] jarFiles = directory.listFiles(file ->
                file.isFile()
                        && file.getName().toLowerCase(Locale.ROOT).endsWith(".jar"));

        if (jarFiles == null) {
            log.error("Could not read plugin directory {}", directory);
            return;
        }

        Arrays.sort(jarFiles, Comparator.comparing(File::getName));

        for (File jarFile : jarFiles) {
            try {
                RunningPlugin runningPlugin = startPlugin(jarFile.toPath());

                RunningPlugin existing = runningPlugins.putIfAbsent(
                        runningPlugin.id(),
                        runningPlugin
                );

                if (existing != null) {
                    log.error(
                            "Duplicate plugin id '{}' in {}. Already loaded from {}",
                            runningPlugin.id(),
                            runningPlugin.pathToJar(),
                            existing.pathToJar()
                    );
                    stopPlugin(runningPlugin);
                }
            } catch (Exception | LinkageError | ServiceConfigurationError e) {
                log.error("Failed to start plugin from {}", jarFile, e);
            }
        }
    }

    private RunningPlugin startPlugin(Path jarPath) throws IOException {
        PluginLoader.LoadedPlugin loaded = pluginLoader.load(jarPath);
        ThreadPoolExecutor eventWorker = null;
        boolean enableAttempted = false;

        try {
            Plugin plugin = loaded.plugin();
            String pluginId = plugin.getId();

            if (pluginId == null || pluginId.isBlank()) {
                throw new IllegalArgumentException(
                        "Plugin id must not be null or blank: " + jarPath
                );
            }

            if (runningPlugins.containsKey(pluginId)) {
                throw new IllegalArgumentException(
                        "Plugin id is already loaded: " + pluginId
                );
            }

            eventWorker = createBalanceEventWorker(pluginId);

            Map<String, String> values = pluginProperties.getPlugins()
                    .getOrDefault(pluginId, Map.of());

            PluginSettings settings = new DefaultPluginSettings(values);
            DefaultPluginContext context =
                    new DefaultPluginContext(userDirectory, settings);

            enableAttempted = true;
            plugin.onEnable(context);

            log.info("Plugin '{}' enabled from {}", pluginId, jarPath);

            return new RunningPlugin(
                    pluginId,
                    jarPath,
                    loaded,
                    context,
                    eventWorker
            );
        } catch (RuntimeException | LinkageError | ServiceConfigurationError e) {
            if (eventWorker != null) {
                shutdownWorker(loaded.plugin().getId(), eventWorker);
            }

            if (enableAttempted) {
                disableAfterFailedStart(loaded.plugin(), jarPath);
            }

            closeQuietly(loaded);
            throw e;
        }
    }

    private ThreadPoolExecutor createBalanceEventWorker(String pluginId) {
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(
                    task,
                    "plugin-" + pluginId + "-balance-events"
            );
            thread.setDaemon(true);
            return thread;
        };

        return new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(eventQueueCapacity),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public void dispatchBalanceChanged(BalanceChanged event) {
        for (RunningPlugin runningPlugin : runningPlugins.values()) {
            List<Consumer<BalanceChanged>> handlers =
                    runningPlugin.defaultPluginContext()
                            .getBalanceChangedHandlers();

            if (handlers.isEmpty()) {
                continue;
            }

            try {
                runningPlugin.balanceEventWorker().execute(() -> {
                    for (Consumer<BalanceChanged> handler : handlers) {
                        try {
                            handler.accept(event);
                        } catch (Exception | LinkageError e) {
                            log.error(
                                    "Balance change handler failed in plugin '{}'",
                                    runningPlugin.id(),
                                    e
                            );
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                if (runningPlugin.balanceEventWorker().isShutdown()) {
                    log.debug(
                            "Event was not queued because plugin '{}' is stopping",
                            runningPlugin.id()
                    );
                } else {
                    log.error(
                            "Balance event queue is full for plugin '{}'; rejecting ledgerId={}",
                            runningPlugin.id(),
                            event.ledgerId()
                    );
                }
            }
        }
    }

    @PreDestroy
    void stopPlugins() {
        List<RunningPlugin> pluginsToStop =
                new ArrayList<>(runningPlugins.values());

        for (RunningPlugin plugin : pluginsToStop) {
            stopPlugin(plugin);
        }
    }

    private void stopPlugin(RunningPlugin runningPlugin) {
        runningPlugins.remove(runningPlugin.id(), runningPlugin);

        try {
            shutdownWorker(
                    runningPlugin.id(),
                    runningPlugin.balanceEventWorker()
            );

            runningPlugin.loadedPlugin().plugin().onDisable();
            log.info("Plugin '{}' disabled", runningPlugin.id());
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            log.error("Plugin '{}' failed on disable", runningPlugin.id(), e);
        } finally {
            closeQuietly(runningPlugin.loadedPlugin());
        }
    }

    private void shutdownWorker(
            String pluginId,
            ThreadPoolExecutor worker
    ) {
        worker.shutdown();

        try {
            if (!worker.awaitTermination(
                    WORKER_SHUTDOWN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            )) {
                int pendingTasks = worker.shutdownNow().size();

                log.warn(
                        "Worker for plugin '{}' did not stop in time; "
                                + "interrupted it and cancelled {} queued task(s)",
                        pluginId,
                        pendingTasks
                );

                if (!worker.awaitTermination(1, TimeUnit.SECONDS)) {
                    log.error(
                            "Worker for plugin '{}' is still running after interruption",
                            pluginId
                    );
                }
            }
        } catch (InterruptedException e) {
            worker.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("Interrupted while stopping worker for plugin '{}'", pluginId, e);
        }
    }

    private void disableAfterFailedStart(
            Plugin plugin,
            Path jarPath
    ) {
        try {
            plugin.onDisable();
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            log.error("Plugin cleanup failed after startup error: {}", jarPath, e);
        }
    }

    private void closeQuietly(PluginLoader.LoadedPlugin plugin) {
        try {
            plugin.classLoader().close();
        } catch (IOException e) {
            log.warn(
                    "Failed to close class loader for plugin '{}'",
                    plugin.plugin().getId(),
                    e
            );
        }
    }
}