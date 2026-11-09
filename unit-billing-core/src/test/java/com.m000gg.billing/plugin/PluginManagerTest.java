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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void startPluginsReturnsWhenDirectoryIsNotConfigured() {
        PluginManager manager = new PluginManager(null, null, null);

        assertDoesNotThrow(manager::startPlugins);
    }

    @Test
    void startPluginsDoesNotCreateDirectoryWhenQueueCapacityIsInvalid() throws Exception {
        Path pluginDirectory = tempDir.resolve("plugins");
        PluginManager manager = new PluginManager(null, null, null);
        setField(manager, "pluginDirectory", pluginDirectory.toString());
        setField(manager, "eventQueueCapacity", 0);

        manager.startPlugins();

        assertFalse(Files.exists(pluginDirectory));
    }

    @Test
    void startPluginsCreatesConfiguredDirectoryWhenNoJarsArePresent() throws Exception {
        Path pluginDirectory = tempDir.resolve("plugins");
        PluginManager manager = new PluginManager(null, null, null);
        setField(manager, "pluginDirectory", pluginDirectory.toString());
        setField(manager, "eventQueueCapacity", 1);

        manager.startPlugins();

        assertTrue(Files.isDirectory(pluginDirectory));
    }

    @Test
    void dispatchBalanceChangedDoesNothingWhenNoPluginsAreRunning() {
        PluginManager manager = new PluginManager(null, null, null);

        assertDoesNotThrow(() -> manager.dispatchBalanceChanged(null));
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
}