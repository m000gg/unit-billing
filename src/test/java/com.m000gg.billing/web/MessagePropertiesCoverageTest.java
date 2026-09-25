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

package com.m000gg.billing.web;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;


class MessagePropertiesCoverageTest {

    private static final List<String> BASENAMES = List.of(
            "admin", "client", "subscribers", "ledger", "identity", "catalog", "subscriptions", "common", "errors"
    );
    private static final List<String> LOCALES = List.of("en", "ru", "uk", "de", "fr");

    @Test
    void allLocalesShouldHaveSameKeysPerBasename() throws IOException {
        for (String basename : BASENAMES) {
            Map<String, Set<String>> keysByLocale = new HashMap<>();

            for (String locale : LOCALES) {
                String path = locale.equals("en")
                        ? "i18n/%s.properties".formatted(basename)
                        : "i18n/%s_%s.properties".formatted(basename, locale);
                Properties props = new Properties();
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                    if (is == null) continue;
                    props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                }
                keysByLocale.put(locale, props.stringPropertyNames());
            }

            Set<String> referenceKeys = keysByLocale.getOrDefault("en", Set.of());
            for (String locale : LOCALES) {
                if (locale.equals("en")) continue;
                Set<String> missing = new HashSet<>(referenceKeys);
                missing.removeAll(keysByLocale.getOrDefault(locale, Set.of()));
                assertThat(missing)
                        .withFailMessage("Basename '%s', locale '%s' missing keys: %s", basename, locale, missing)
                        .isEmpty();
            }
        }
    }
}
