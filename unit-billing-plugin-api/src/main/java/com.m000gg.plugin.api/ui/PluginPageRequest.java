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

package com.m000gg.plugin.api.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A request to a page of a plugin.
 *
 * @param method            HTTP method, {@code GET} or {@code POST}
 * @param path              path inside the plugin namespace, starts with '/' ('/' for the root page)
 * @param parameters        query and form parameters (immutable)
 * @param userId            id of the current user: the admin in the admin area, the subscriber in the client area
 * @param language          language code of the current locale, for example {@code en}
 * @param csrfParameterName name of the hidden form field that carries the CSRF token
 * @param csrfToken         CSRF token; every {@code POST} form must send it
 * @param assetsBasePath    base path of the plugin's static files, for example {@code /plugin-assets/subscriptions}
 */
public record PluginPageRequest(String method,
                                String path,
                                Map<String, List<String>> parameters,
                                UUID userId,
                                String language,
                                String csrfParameterName,
                                String csrfToken,
                                String assetsBasePath) {

    public PluginPageRequest {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(csrfParameterName, "csrfParameterName");
        Objects.requireNonNull(csrfToken, "csrfToken");
        Objects.requireNonNull(assetsBasePath, "assetsBasePath");

        Map<String, List<String>> copy = new LinkedHashMap<>();
        parameters.forEach((name, values) -> copy.put(name, List.copyOf(values)));
        parameters = Collections.unmodifiableMap(copy);
    }

    /** @return the first value of the parameter, if present */
    public Optional<String> parameter(String name) {
        List<String> values = parameters.get(name);
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }
}
