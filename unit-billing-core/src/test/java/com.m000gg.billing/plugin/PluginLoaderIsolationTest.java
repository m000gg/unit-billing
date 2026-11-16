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
import com.m000gg.plugin.api.PluginContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.lang.constant.ConstantDescs.CD_Class;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_boolean;
import static java.lang.constant.ConstantDescs.CD_void;
import static java.lang.constant.ConstantDescs.INIT_NAME;
import static java.lang.constant.ConstantDescs.MTD_void;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PluginLoaderIsolationTest {

    @TempDir
    Path tempDir;

    private final PluginLoader pluginLoader = new PluginLoader();
    private final PluginBytecodeValidator bytecodeValidator = new PluginBytecodeValidator();

    @Test
    void loadsValidPluginAndBlocksHostAndJdbcClassesFromPluginClassLoader() throws Exception {
        Path jarPath = buildValidPluginJar("com/example/ValidTestPlugin", "valid-test");

        PluginLoader.LoadedPlugin loaded = pluginLoader.load(jarPath);
        try (var classLoader = loaded.classLoader()) {
            assertEquals("valid-test", loaded.plugin().getId());

            assertThrows(ClassNotFoundException.class, () ->
                    Class.forName("com.m000gg.billing.plugin.PluginManager", false, classLoader));
            assertThrows(ClassNotFoundException.class, () ->
                    Class.forName("java.sql.DriverManager", false, classLoader));
            assertThrows(ClassNotFoundException.class, () ->
                    Class.forName("javax.sql.DataSource", false, classLoader));
            assertThrows(ClassNotFoundException.class, () ->
                    Class.forName("org.postgresql.Driver", false, classLoader));
            assertThrows(ClassNotFoundException.class, () ->
                    Class.forName("org.springframework.jdbc.core.JdbcTemplate", false, classLoader));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "java.sql.DriverManager",
            "javax.sql.DataSource",
            "org.postgresql.Driver",
            "com.m000gg.billing.ledger.LedgerService",
            "org.springframework.jdbc.core.JdbcTemplate",
            "jakarta.persistence.EntityManager"
    })
    void rejectsPluginJarReferencingForbiddenClass(String forbiddenBinaryName) throws Exception {
        byte[] badClass = ClassFile.of().build(
                ClassDesc.of("com.example.BadPlugin"),
                cb -> cb.withMethodBody("touch", MTD_void, ClassFile.ACC_PUBLIC, code -> {
                    code.aconst_null();
                    code.checkcast(ClassDesc.of(forbiddenBinaryName));
                    code.pop();
                    code.return_();
                })
        );

        Path jarPath = tempDir.resolve("bad-" + forbiddenBinaryName + ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("com/example/BadPlugin.class"));
            jos.write(badClass);
            jos.closeEntry();
        }

        assertThrows(SecurityException.class, () -> pluginLoader.load(jarPath));
    }

    @Test
    void rejectsMethodHandlesPrivateLookupInAndReflectionInPluginCode() throws Exception {
        ClassDesc methodHandles = ClassDesc.of("java.lang.invoke.MethodHandles");
        ClassDesc lookup = ClassDesc.of("java.lang.invoke.MethodHandles$Lookup");
        byte[] lookupUser = ClassFile.of().build(
                ClassDesc.of("com.m000gg.plugins.evil.LookupPlugin"),
                cb -> cb.withMethodBody("hack", MTD_void, ClassFile.ACC_PUBLIC, code -> {
                    code.aconst_null();
                    code.aconst_null();
                    code.invokestatic(
                            methodHandles,
                            "privateLookupIn",
                            MethodTypeDesc.of(lookup, CD_Class, lookup)
                    );
                    code.pop();
                    code.return_();
                })
        );
        assertThrows(SecurityException.class, () ->
                bytecodeValidator.validateClassBytes("com/m000gg/plugins/evil/LookupPlugin.class", lookupUser));

        byte[] reflectUser = ClassFile.of().build(
                ClassDesc.of("com.m000gg.plugins.evil.ReflectPlugin"),
                cb -> cb.withMethodBody("hack", MTD_void, ClassFile.ACC_PUBLIC, code -> {
                    code.aconst_null();
                    code.iconst_1();
                    code.invokevirtual(
                            ClassDesc.of("java.lang.reflect.Field"),
                            "setAccessible",
                            MethodTypeDesc.of(CD_void, CD_boolean)
                    );
                    code.return_();
                })
        );
        assertThrows(SecurityException.class, () ->
                bytecodeValidator.validateClassBytes("com/m000gg/plugins/evil/ReflectPlugin.class", reflectUser));
    }

    @Test
    void allowsStringConcatenationAndLambdasUsingIndy() {
        ClassDesc stringConcatFactory = ClassDesc.of("java.lang.invoke.StringConcatFactory");
        ClassDesc lookup = ClassDesc.of("java.lang.invoke.MethodHandles$Lookup");
        DirectMethodHandleDesc bsm = MethodHandleDesc.ofMethod(
                DirectMethodHandleDesc.Kind.STATIC,
                stringConcatFactory,
                "makeConcatWithConstants",
                MethodTypeDesc.of(
                        ClassDesc.of("java.lang.invoke.CallSite"),
                        lookup,
                        CD_String,
                        ClassDesc.of("java.lang.invoke.MethodType"),
                        CD_String,
                        CD_Object.arrayType()
                )
        );
        byte[] concatClass = ClassFile.of().build(
                ClassDesc.of("com.m000gg.plugins.ok.Greeting"),
                cb -> cb.withMethodBody("greet", MethodTypeDesc.of(CD_String, CD_String), ClassFile.ACC_PUBLIC, code -> {
                    code.aload(1);
                    code.invokedynamic(DynamicCallSiteDesc.of(
                            bsm,
                            "makeConcatWithConstants",
                            MethodTypeDesc.of(CD_String, CD_String),
                            "hi \u0001"
                    ));
                    code.areturn();
                })
        );

        bytecodeValidator.validateClassBytes("com/m000gg/plugins/ok/Greeting.class", concatClass);
    }

    @Test
    void rejectsClassInReservedHostPackage() {
        byte[] spoofed = ClassFile.of().build(
                ClassDesc.of("com.m000gg.billing.SpoofedHelper"),
                cb -> cb.withMethodBody(INIT_NAME, MTD_void, ClassFile.ACC_PUBLIC, code -> {
                    code.aload(0);
                    code.invokespecial(ClassDesc.of("java.lang.Object"), INIT_NAME, MTD_void);
                    code.return_();
                })
        );

        assertThrows(SecurityException.class,
                () -> bytecodeValidator.validateClassBytes("com/m000gg/billing/SpoofedHelper.class", spoofed));
    }

    private Path buildValidPluginJar(String internalName, String pluginId) throws IOException {
        ClassDesc thisClass = ClassDesc.ofInternalName(internalName);
        ClassDesc objectClass = ClassDesc.of("java.lang.Object");
        ClassDesc pluginInterface = ClassDesc.of(Plugin.class.getName());
        ClassDesc contextClass = ClassDesc.of(PluginContext.class.getName());

        byte[] bytes = ClassFile.of().build(thisClass, cb -> {
            cb.withInterfaceSymbols(pluginInterface);
            cb.withMethodBody(INIT_NAME, MTD_void, ClassFile.ACC_PUBLIC, code -> {
                code.aload(0);
                code.invokespecial(objectClass, INIT_NAME, MTD_void);
                code.return_();
            });
            cb.withMethodBody("getId", MethodTypeDesc.of(CD_String), ClassFile.ACC_PUBLIC, code -> {
                code.ldc(pluginId);
                code.areturn();
            });
            cb.withMethodBody("onEnable", MethodTypeDesc.of(CD_void, contextClass), ClassFile.ACC_PUBLIC, code -> {
                code.return_();
            });
            cb.withMethodBody("onDisable", MTD_void, ClassFile.ACC_PUBLIC, code -> {
                code.return_();
            });
        });

        Path jarPath = tempDir.resolve(pluginId + ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry(internalName + ".class"));
            jos.write(bytes);
            jos.closeEntry();

            jos.putNextEntry(new JarEntry("META-INF/services/" + Plugin.class.getName()));
            jos.write((internalName.replace('/', '.') + "\n").getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return jarPath;
    }
}
