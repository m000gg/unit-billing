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
import com.m000gg.billing.plugin.context.DefaultPluginUi;
import com.m000gg.plugin.api.Plugin;
import com.m000gg.plugin.api.command.balance.BalanceCharger;
import com.m000gg.plugin.api.event.BalanceChanged;
import com.m000gg.plugin.api.query.settings.PluginSettings;
import com.m000gg.plugin.api.query.user.UserDirectory;
import com.m000gg.plugin.api.ui.PluginPageHandler;
import com.m000gg.plugin.api.ui.PluginPageRequest;
import com.m000gg.plugin.api.ui.PluginPageResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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
    private final BalanceCharger balanceCharger;
    private final PluginLoader pluginLoader = new PluginLoader();

    private final ConcurrentMap<String, RunningPlugin> runningPlugins = new ConcurrentHashMap<>();
    private static final Pattern PLUGIN_ID_PATTERN = Pattern.compile("[a-z0-9-]+");
    private static final String ASSET_FOLDER = "plugin-static/";

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

            validatePluginId(pluginId);

            if (runningPlugins.containsKey(pluginId)) {
                throw new IllegalArgumentException(
                        "Plugin id is already loaded: " + pluginId
                );
            }

            eventWorker = createBalanceEventWorker(pluginId);

            Map<String, String> values = pluginProperties.getPlugins()
                    .getOrDefault(pluginId, Map.of());

            PluginSettings settings = new DefaultPluginSettings(values);
            DefaultPluginContext context = new DefaultPluginContext(userDirectory, settings, balanceCharger);

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
                        } catch (Exception | LinkageError | ServiceConfigurationError e) {
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

    public PluginPageResponse renderPage(String pluginId, PluginArea area, PluginPageRequest request) {
        if (stuckPlugins.containsKey(pluginId)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }

        RunningPlugin runningPlugin = runningPlugins.get(pluginId);
        if (runningPlugin == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        DefaultPluginUi ui = runningPlugin.defaultPluginContext().getPluginUi();
        PluginPageHandler handler = (area == PluginArea.ADMIN
                ? ui.getAdminPageHandler()
                : ui.getClientPageHandler())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(runningPlugin.loadedPlugin().classLoader());
        try {
            PluginPageResponse response = handler.handle(request);
            if (response == null) {
                throw new IllegalStateException("Page handler returned null");
            }
            return response;
        } catch (Exception | LinkageError e) {
            log.error("Plugin '{}' failed to render the {} page '{}'", pluginId, area, request.path(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    public Optional<byte[]> readAsset(String pluginId, String assetPath) {
        RunningPlugin runningPlugin = runningPlugins.get(pluginId);
        if (runningPlugin == null) {
            return Optional.empty();
        }

        String relative = assetPath.startsWith("/") ? assetPath.substring(1) : assetPath;
        if (relative.isEmpty() || relative.contains("..")
                || relative.contains("\\") || relative.contains("//")) {
            return Optional.empty();
        }

        URL url = runningPlugin.loadedPlugin().classLoader().findResource(ASSET_FOLDER + relative);
        if (url == null) {
            return Optional.empty();
        }

        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            try (InputStream in = connection.getInputStream()) {
                return Optional.of(in.readAllBytes());
            }
        } catch (IOException e) {
            log.warn("Could not read asset '{}' of plugin '{}'", relative, pluginId, e);
            return Optional.empty();
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

    private final ConcurrentMap<String, RunningPlugin> stuckPlugins = new ConcurrentHashMap<>();

    private void stopPlugin(RunningPlugin runningPlugin) {
        runningPlugins.remove(runningPlugin.id(), runningPlugin);

        if (!shutdownWorker(runningPlugin.id(), runningPlugin.balanceEventWorker())) {
            stuckPlugins.put(runningPlugin.id(), runningPlugin);
            log.error(
                    "Plugin '{}' worker is stuck; skipping onDisable() and keeping class loader open",
                    runningPlugin.id()
            );
            return;
        }

        try {
            runningPlugin.loadedPlugin().plugin().onDisable();
            log.info("Plugin '{}' disabled", runningPlugin.id());
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            log.error("Plugin '{}' failed on disable", runningPlugin.id(), e);
        } finally {
            closeQuietly(runningPlugin.loadedPlugin());
        }
    }

    /**
     * Stops plugin worker.
     *
     * @return true, if worker definitely stopped;
     *         false,if is still can do plugin code
     */
    private boolean shutdownWorker(
            String pluginId,
            ThreadPoolExecutor worker
    ) {
        worker.shutdown();

        try {
            if (worker.awaitTermination(
                    WORKER_SHUTDOWN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            )) {
                return true;
            }

            int pendingTasks = worker.shutdownNow().size();

            log.warn(
                    "Worker for plugin '{}' did not stop in time; "
                            + "interrupted it and cancelled {} queued task(s)",
                    pluginId,
                    pendingTasks
            );

            if (worker.awaitTermination(1, TimeUnit.SECONDS)) {
                return true;
            }

            log.error(
                    "Worker for plugin '{}' is still running after interruption",
                    pluginId
            );
            return false;
        } catch (InterruptedException e) {
            worker.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("Interrupted while stopping worker for plugin '{}'", pluginId, e);
            return false;
        }
    }

    public List<AdminNavigationItem> getAdminNavigationItems() {
        return runningPlugins.values()
                .stream()
                .map(runningPlugin -> {
                    DefaultPluginUi ui = runningPlugin
                            .defaultPluginContext()
                            .getPluginUi();

                    if (ui.getAdminPageHandler().isEmpty()) {
                        return Optional.<AdminNavigationItem>empty();
                    }

                    return ui.getAdminNavigation()
                            .map(navigation -> new AdminNavigationItem(
                                    runningPlugin.id(),
                                    navigation.title(),
                                    navigation.icon(),
                                    "/admin/plugins/" + runningPlugin.id() + "/"
                            ));
                })
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(
                        AdminNavigationItem::title,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
    }

    public List<String> getClientPluginIds() {
        return runningPlugins.values()
                .stream()
                .filter(plugin -> plugin.defaultPluginContext()
                        .getPluginUi()
                        .getClientPageHandler()
                        .isPresent())
                .map(RunningPlugin::id)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
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

    static void validatePluginId(String pluginId) {
        if (pluginId == null || !PLUGIN_ID_PATTERN.matcher(pluginId).matches()) {
            throw new IllegalArgumentException("Plugin id must match [a-z0-9-]+: " + pluginId);
        }
    }

    public record AdminNavigationItem(
            String pluginId,
            String title,
            String icon,
            String href
    ) {
    }
}