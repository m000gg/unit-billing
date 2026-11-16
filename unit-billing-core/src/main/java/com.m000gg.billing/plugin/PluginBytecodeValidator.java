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

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Inspects plugin JAR bytecode at load time using Java's {@link ClassFile} API
 * and rejects plugins that reference core internals, direct JDBC/PostgreSQL APIs,
 * ORM frameworks, or reflection primitives that could bypass host isolation.
 */
public class PluginBytecodeValidator {

    private static final List<String> FORBIDDEN_PREFIXES = List.of(
            "com/m000gg/billing/",
            "java/sql/",
            "javax/sql/",
            "org/postgresql/",
            "org/springframework/",
            "jakarta/persistence/",
            "org/hibernate/",
            "org/flywaydb/",
            "com/zaxxer/hikari/",
            "java/lang/reflect/",
            "jdk/internal/"
    );

    private static final List<String> FORBIDDEN_EXACT_CLASSES = List.of(
            "java/lang/invoke/MethodHandles",
            "sun/misc/Unsafe"
    );

    private static final List<String> RESERVED_PLUGIN_PACKAGE_PREFIXES = List.of(
            "com/m000gg/billing/",
            "com/m000gg/plugin/api/"
    );

    public void validateJar(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                if (entry.getName().endsWith("module-info.class")) {
                    continue;
                }
                try (InputStream in = jarFile.getInputStream(entry)) {
                    validateClassBytes(entry.getName(), in.readAllBytes());
                }
            }
        }
    }

    void validateClassBytes(String entryName, byte[] classBytes) {
        ClassModel model = ClassFile.of().parse(classBytes);
        String thisClass = model.thisClass().asInternalName();

        for (String reservedPrefix : RESERVED_PLUGIN_PACKAGE_PREFIXES) {
            if (thisClass.startsWith(reservedPrefix)) {
                throw new SecurityException(
                        "Plugin JAR must not define classes in reserved package: " + thisClass);
            }
        }

        for (PoolEntry poolEntry : model.constantPool()) {
            if (poolEntry instanceof ClassEntry classEntry) {
                String internalName = unwrapArrayType(classEntry.asInternalName());
                if (isForbiddenClass(internalName)) {
                    throw new SecurityException(
                            "Plugin class '" + entryName + "' references forbidden API '"
                                    + internalName.replace('/', '.') + "'");
                }
            }
        }
    }

    private static String unwrapArrayType(String internalName) {
        if (!internalName.startsWith("[")) {
            return internalName;
        }
        int idx = 0;
        while (idx < internalName.length() && internalName.charAt(idx) == '[') {
            idx++;
        }
        if (idx < internalName.length() && internalName.charAt(idx) == 'L' && internalName.endsWith(";")) {
            return internalName.substring(idx + 1, internalName.length() - 1);
        }
        return internalName;
    }

    private static boolean isForbiddenClass(String internalName) {
        for (String prefix : FORBIDDEN_PREFIXES) {
            if (internalName.startsWith(prefix)) {
                return true;
            }
        }
        return FORBIDDEN_EXACT_CLASSES.contains(internalName);
    }
}
