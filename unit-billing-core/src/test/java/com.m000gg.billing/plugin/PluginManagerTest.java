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

import com.m000gg.billing.plugin.context.DefaultExchangeRateDirectory;
import com.m000gg.billing.plugin.context.PluginExchangeRateRegistry;
import com.m000gg.billing.plugin.db.PluginDatabaseFactory;
import com.m000gg.billing.settings.SystemSettingService;
import com.m000gg.plugin.api.db.PluginDatabase;
import com.m000gg.plugin.api.query.fx.ExchangeRate;
import com.m000gg.plugin.api.query.fx.ExchangeRateDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void startPluginsReturnsWhenDirectoryIsNotConfigured() {
        PluginManager manager = newEmptyManager();

        assertDoesNotThrow(manager::startPlugins);
    }

    @Test
    void startPluginsDoesNotCreateDirectoryWhenQueueCapacityIsInvalid() throws Exception {
        Path pluginDirectory = tempDir.resolve("plugins");
        PluginManager manager = newEmptyManager();
        setField(manager, "pluginDirectory", pluginDirectory.toString());
        setField(manager, "eventQueueCapacity", 0);

        manager.startPlugins();

        assertFalse(Files.exists(pluginDirectory));
    }

    @Test
    void startPluginsCreatesConfiguredDirectoryWhenNoJarsArePresent() throws Exception {
        Path pluginDirectory = tempDir.resolve("plugins");
        PluginManager manager = newEmptyManager();
        setField(manager, "pluginDirectory", pluginDirectory.toString());
        setField(manager, "eventQueueCapacity", 1);

        manager.startPlugins();

        assertTrue(Files.isDirectory(pluginDirectory));
    }

    @Test
    void dispatchBalanceChangedDoesNothingWhenNoPluginsAreRunning() {
        PluginManager manager = newEmptyManager();

        assertDoesNotThrow(() -> manager.dispatchBalanceChanged(null));
    }

    private static PluginManager newEmptyManager() {
        return new PluginManager(null, null, null, null, null, null, null);
    }

    @Test
    void loadsFxProviderPluginServesRatesAcrossPluginsRegardlessOfStartOrderAndUnregistersOnStop() throws Exception {
        Path pluginDirectory = Files.createDirectories(tempDir.resolve("plugins"));
        buildPluginJar(
                pluginDirectory.resolve("aaa-consumer-plugin.jar"),
                "com.example.consumer.ConsumerTestPlugin",
                """
                package com.example.consumer;

                import com.m000gg.plugin.api.Plugin;
                import com.m000gg.plugin.api.PluginContext;
                import com.m000gg.plugin.api.query.fx.ExchangeRateDirectory;

                public class ConsumerTestPlugin implements Plugin {
                    private ExchangeRateDirectory exchangeRates;

                    @Override
                    public String getId() {
                        return "consumer";
                    }

                    @Override
                    public void onEnable(PluginContext context) {
                        this.exchangeRates = context.exchangeRates();
                    }

                    @Override
                    public void onDisable() {
                    }

                    public String queryGbpToEur() {
                        return exchangeRates.findRate("GBP", "EUR")
                                .map(rate -> rate.rate().toPlainString() + ":" + rate.source())
                                .orElse("missing");
                    }
                }
                """
        );
        buildPluginJar(
                pluginDirectory.resolve("nbu-fx-plugin.jar"),
                "com.example.fx.NbuTestPlugin",
                """
                package com.example.fx;

                import com.m000gg.plugin.api.Plugin;
                import com.m000gg.plugin.api.PluginContext;
                import com.m000gg.plugin.api.query.fx.ExchangeRate;
                import java.math.BigDecimal;
                import java.time.Instant;
                import java.util.Optional;

                public class NbuTestPlugin implements Plugin {
                    @Override
                    public String getId() {
                        return "nbu-fx";
                    }

                    @Override
                    public void onEnable(PluginContext context) {
                        context.registerExchangeRateProvider((from, to) -> {
                            if ("GBP".equals(from) && "EUR".equals(to)) {
                                return Optional.of(new ExchangeRate(new BigDecimal("1.18"), "nbu", Instant.now()));
                            }
                            return Optional.empty();
                        });
                    }

                    @Override
                    public void onDisable() {
                    }
                }
                """
        );

        PluginExchangeRateRegistry registry = new PluginExchangeRateRegistry();
        ExchangeRateDirectory directory = new DefaultExchangeRateDirectory(
                new SystemSettingService() {
                    @Override
                    public String getBaseCurrency() {
                        return "USD";
                    }
                },
                registry
        );
        PluginManager manager = newManagerWithDir(pluginDirectory, directory, registry);

        manager.startPlugins();

        Optional<ExchangeRate> quote = directory.findRate("GBP", "EUR");
        assertTrue(quote.isPresent());
        assertEquals(0, new BigDecimal("1.18").compareTo(quote.get().rate()));
        assertEquals("nbu", quote.get().source());
        assertEquals("USD", directory.baseCurrency());

        // Verify a consumer plugin loaded before the FX provider resolves the rate dynamically.
        var runningField = PluginManager.class.getDeclaredField("runningPlugins");
        runningField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var running = (java.util.Map<String, RunningPlugin>) runningField.get(manager);
        Object consumerPlugin = running.get("consumer").loadedPlugin().plugin();
        assertEquals("1.18:nbu", consumerPlugin.getClass().getMethod("queryGbpToEur").invoke(consumerPlugin));

        manager.stopPlugins();

        assertTrue(registry.getActiveProviderPluginId().isEmpty());
        assertTrue(directory.findRate("GBP", "EUR").isEmpty());
    }

    @Test
    void unregistersFxProviderWhenPluginFailsDuringOnEnable() throws Exception {
        Path pluginDirectory = Files.createDirectories(tempDir.resolve("plugins"));
        buildPluginJar(
                pluginDirectory.resolve("broken-fx-plugin.jar"),
                "com.example.fx.BrokenFxPlugin",
                """
                package com.example.fx;

                import com.m000gg.plugin.api.Plugin;
                import com.m000gg.plugin.api.PluginContext;
                import com.m000gg.plugin.api.query.fx.ExchangeRate;
                import java.math.BigDecimal;
                import java.time.Instant;
                import java.util.Optional;

                public class BrokenFxPlugin implements Plugin {
                    @Override
                    public String getId() {
                        return "broken-fx";
                    }

                    @Override
                    public void onEnable(PluginContext context) {
                        context.registerExchangeRateProvider((from, to) ->
                                Optional.of(new ExchangeRate(BigDecimal.TEN, "broken", Instant.now())));
                        throw new IllegalStateException("startup failed after registration");
                    }

                    @Override
                    public void onDisable() {
                    }
                }
                """
        );

        PluginExchangeRateRegistry registry = new PluginExchangeRateRegistry();
        ExchangeRateDirectory directory = new DefaultExchangeRateDirectory(null, registry);
        PluginManager manager = newManagerWithDir(pluginDirectory, directory, registry);

        manager.startPlugins();

        assertTrue(registry.getActiveProviderPluginId().isEmpty());
        assertTrue(directory.findRate("USD", "EUR").isEmpty());
    }

    @Test
    void rejectingDuplicatePluginDoesNotUnregisterActiveFxProvider() throws Exception {
        Path pluginDirectory = Files.createDirectories(tempDir.resolve("plugins"));
        String source = """
                package com.example.fx;

                import com.m000gg.plugin.api.Plugin;
                import com.m000gg.plugin.api.PluginContext;
                import com.m000gg.plugin.api.query.fx.ExchangeRate;
                import java.math.BigDecimal;
                import java.time.Instant;
                import java.util.Optional;

                public class DupFxPlugin implements Plugin {
                    @Override
                    public String getId() {
                        return "nbu-fx";
                    }

                    @Override
                    public void onEnable(PluginContext context) {
                        context.registerExchangeRateProvider((from, to) ->
                                Optional.of(new ExchangeRate(new BigDecimal("41.50"), "nbu", Instant.now())));
                    }

                    @Override
                    public void onDisable() {
                    }
                }
                """;
        buildPluginJar(pluginDirectory.resolve("a-nbu-fx.jar"), "com.example.fx.DupFxPlugin", source);
        buildPluginJar(pluginDirectory.resolve("b-nbu-fx-dup.jar"), "com.example.fx.DupFxPlugin", source);

        PluginExchangeRateRegistry registry = new PluginExchangeRateRegistry();
        ExchangeRateDirectory directory = new DefaultExchangeRateDirectory(null, registry);
        PluginManager manager = newManagerWithDir(pluginDirectory, directory, registry);

        manager.startPlugins();
        try {
            assertEquals(Optional.of("nbu-fx"), registry.getActiveProviderPluginId());
            Optional<ExchangeRate> quote = directory.findRate("USD", "UAH");
            assertTrue(quote.isPresent());
            assertEquals(0, new BigDecimal("41.50").compareTo(quote.get().rate()));
        } finally {
            manager.stopPlugins();
        }
    }

    @Test
    void pluginCannotReRegisterFxProviderDuringOnDisable() throws Exception {
        Path pluginDirectory = Files.createDirectories(tempDir.resolve("plugins"));
        buildPluginJar(
                pluginDirectory.resolve("re-register-on-disable.jar"),
                "com.example.fx.ReRegisterOnDisablePlugin",
                """
                package com.example.fx;

                import com.m000gg.plugin.api.Plugin;
                import com.m000gg.plugin.api.PluginContext;
                import com.m000gg.plugin.api.query.fx.ExchangeRate;
                import java.math.BigDecimal;
                import java.time.Instant;
                import java.util.Optional;

                public class ReRegisterOnDisablePlugin implements Plugin {
                    private PluginContext retainedContext;

                    @Override
                    public String getId() {
                        return "sneaky-fx";
                    }

                    @Override
                    public void onEnable(PluginContext context) {
                        this.retainedContext = context;
                        context.registerExchangeRateProvider((from, to) ->
                                Optional.of(new ExchangeRate(BigDecimal.TWO, "sneaky", Instant.now())));
                    }

                    @Override
                    public void onDisable() {
                        retainedContext.registerExchangeRateProvider((from, to) ->
                                Optional.of(new ExchangeRate(BigDecimal.TEN, "sneaky-after-stop", Instant.now())));
                    }
                }
                """
        );

        PluginExchangeRateRegistry registry = new PluginExchangeRateRegistry();
        ExchangeRateDirectory directory = new DefaultExchangeRateDirectory(null, registry);
        PluginManager manager = newManagerWithDir(pluginDirectory, directory, registry);

        manager.startPlugins();
        assertEquals(Optional.of("sneaky-fx"), registry.getActiveProviderPluginId());

        manager.stopPlugins();
        assertTrue(registry.getActiveProviderPluginId().isEmpty());
        assertTrue(directory.findRate("USD", "EUR").isEmpty());
    }

    private static PluginManager newManagerWithDir(
            Path pluginDirectory,
            ExchangeRateDirectory directory,
            PluginExchangeRateRegistry registry
    ) throws ReflectiveOperationException {
        PluginMigrationRunner noOpMigrations = new PluginMigrationRunner(null, null) {
            @Override
            public int migrate(String pluginId, ClassLoader pluginClassLoader) {
                return 0;
            }
        };
        PluginDatabaseFactory noOpDatabaseFactory = new PluginDatabaseFactory(null, null, null) {
            @Override
            public PluginDatabase create(String pluginId) {
                return null;
            }
        };
        PluginManager manager = new PluginManager(
                null,
                new PluginProperties(),
                null,
                directory,
                registry,
                noOpMigrations,
                noOpDatabaseFactory
        );
        setField(manager, "pluginDirectory", pluginDirectory.toString());
        setField(manager, "eventQueueCapacity", 4);
        return manager;
    }

    private void buildPluginJar(Path targetJar, String className, String source) throws IOException {
        Path workDir = Files.createTempDirectory(tempDir, "compile");
        Path srcFile = workDir.resolve(className.replace('.', '/') + ".java");
        Files.createDirectories(srcFile.getParent());
        Files.writeString(srcFile, source, StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            boolean ok = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    List.of("-d", workDir.toString()),
                    null,
                    fileManager.getJavaFileObjects(srcFile)
            ).call();
            assertTrue(ok, "Failed to compile test plugin " + className);
        }

        try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(targetJar))) {
            jarOut.putNextEntry(new JarEntry("META-INF/services/com.m000gg.plugin.api.Plugin"));
            jarOut.write((className + "\n").getBytes(StandardCharsets.UTF_8));
            jarOut.closeEntry();

            try (var paths = Files.walk(workDir)) {
                for (Path p : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                    String entryName = workDir.relativize(p).toString().replace('\\', '/');
                    jarOut.putNextEntry(new JarEntry(entryName));
                    jarOut.write(Files.readAllBytes(p));
                    jarOut.closeEntry();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"subscriptions", "email-notifier", "hello-ui", "p2p"})
    void validatePluginIdAcceptsLowercaseDigitsAndHyphens(String id) {
        assertDoesNotThrow(() -> PluginManager.validatePluginId(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Subscriptions", "sub_scriptions", "sub:scriptions", "sub/scriptions", "sub scriptions"})
    void validatePluginIdRejectsOtherValues(String id) {
        assertThrows(IllegalArgumentException.class, () -> PluginManager.validatePluginId(id));
    }

    @Test
    void validatePluginIdRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> PluginManager.validatePluginId(null));
    }

    private static void setField(Object target, String fieldName, Object value)
            throws ReflectiveOperationException {
        Field field = PluginManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void adminNavigationIsEmptyWhenNoPluginsAreRunning() {
        PluginManager manager = newEmptyManager();

        assertTrue(manager.getAdminNavigationItems(Locale.ENGLISH).isEmpty());
    }

    @Test
    void clientDashboardSectionsAreEmptyWhenNoPluginsAreRunning() {
        PluginManager manager = newEmptyManager();

        assertTrue(manager.getRenderedClientDashboardSections(Locale.ENGLISH).isEmpty());
    }

    @Test
    void renderPageThrowsNotFoundForUnknownPlugin() {
        PluginManager manager = newEmptyManager();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> manager.renderPage("unknown", PluginArea.ADMIN, null));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void readAssetReturnsEmptyForUnknownPlugin() {
        PluginManager manager = newEmptyManager();

        assertTrue(manager.readAsset("unknown", "style.css").isEmpty());
    }
}