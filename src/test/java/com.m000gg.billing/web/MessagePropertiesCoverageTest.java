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
                String path = "i18n/%s_%s.properties".formatted(basename, locale);
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
