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



import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
final class PluginMessages {

    private static final String BASE = "i18n/messages";
    private static final String MARKER = "{{i18n:";
    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{i18n:([a-zA-Z0-9._-]+)}}");

    private final String pluginId;
    private final URLClassLoader loader;
    private final String defaultLang;
    private final ConcurrentMap<String, Properties> bundles = new ConcurrentHashMap<>();

    PluginMessages(String pluginId, URLClassLoader loader, String defaultLang) {
        this.pluginId = pluginId;
        this.loader = loader;
        this.defaultLang = defaultLang;
    }

    /**
     * @param escapeHtml true,if result is html (pages);
     *                   false, if it is a layout (th:text for title)
     */
    String apply(String text, Locale locale, boolean escapeHtml) {
        if (text == null || !text.contains(MARKER)) {
            return text;
        }
        List<Properties> chain = chainFor(locale);
        return PLACEHOLDER.matcher(text).replaceAll(m -> {
            String key = m.group(1);
            String value = chain.stream()
                    .map(p -> p.getProperty(key))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> {
                        log.warn("Missing i18n key '{}' in plugin '{}'", key, pluginId);
                        return key;
                    });
            return Matcher.quoteReplacement(escapeHtml ? HtmlUtils.htmlEscape(value) : value);
        });
    }

    private List<Properties> chainFor(Locale locale) {
        Set<String> tags = new LinkedHashSet<>();
        if (!locale.getLanguage().isEmpty()) {
            if (!locale.getCountry().isEmpty()) {
                tags.add(locale.getLanguage() + "_" + locale.getCountry());
            }
            tags.add(locale.getLanguage());
        }
        tags.add(defaultLang);
        return tags.stream()
                .map(tag -> bundles.computeIfAbsent(tag, this::loadBundle))
                .toList();
    }

    private Properties loadBundle(String tag) {
        Properties props = new Properties();
        URL url = loader.findResource(BASE + "_" + tag + ".properties");
        if (url == null) {
            return props;
        }
        try {
            URLConnection c = url.openConnection();
            c.setUseCaches(false);
            try (Reader r = new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8)) {
                props.load(r);
            }
        } catch (IOException e) {
            log.warn("Could not read i18n bundle '{}' of plugin '{}'", tag, pluginId, e);
        }
        return props;
    }
}
