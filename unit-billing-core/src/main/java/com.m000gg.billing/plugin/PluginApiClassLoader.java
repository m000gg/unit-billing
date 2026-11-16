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

import java.util.List;

/**
 * Filtering parent {@link ClassLoader} placed above each plugin's {@code URLClassLoader}.
 *
 * <p>Only the public plugin API, safe JDK packages, and SLF4J are visible from the host;
 * core packages, Spring, JPA/Hibernate, Flyway, HikariCP, and JDBC drivers are hidden.
 */
final class PluginApiClassLoader extends ClassLoader {

    private static final List<String> BLOCKED_PREFIXES = List.of(
            "com.m000gg.billing.",
            "java.sql.",
            "javax.sql.",
            "org.postgresql.",
            "org.springframework.",
            "jakarta.persistence.",
            "org.hibernate.",
            "org.flywaydb.",
            "com.zaxxer.hikari.",
            "sun.misc.Unsafe",
            "jdk.internal."
    );

    private static final List<String> ALLOWED_HOST_PREFIXES = List.of(
            "com.m000gg.plugin.api.",
            "org.slf4j.",
            "java.",
            "javax.",
            "sun.",
            "jdk."
    );

    private final ClassLoader hostClassLoader;

    PluginApiClassLoader(ClassLoader hostClassLoader) {
        super(null);
        this.hostClassLoader = hostClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (String blocked : BLOCKED_PREFIXES) {
            if (name.startsWith(blocked)) {
                throw new ClassNotFoundException(
                        "Access to host/JDBC class '" + name + "' is blocked for plugins");
            }
        }

        for (String allowed : ALLOWED_HOST_PREFIXES) {
            if (name.startsWith(allowed)) {
                Class<?> loaded = hostClassLoader.loadClass(name);
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        throw new ClassNotFoundException(name);
    }
}
