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
package com.m000gg.billing.plugin.web;
import com.m000gg.billing.plugin.PluginManager;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class PluginAssetController {

    private final PluginManager pluginManager;

    @GetMapping("/plugin-assets/{pluginId}/**")
    public ResponseEntity<byte[]> getPluginAsset(
            @PathVariable String pluginId,
            HttpServletRequest request
    ) {

        String prefix = request.getContextPath()
                + "/plugin-assets/" + pluginId;

        String assetPath = request.getRequestURI()
                .substring(prefix.length());

        if (assetPath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return pluginManager.readAsset(pluginId, assetPath)
                .map(bytes -> {

                    String mimeType = request.getServletContext()
                            .getMimeType(assetPath);

                    if (mimeType == null) {
                        mimeType = "application/octet-stream";
                    }

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, mimeType)
                            .header("X-Content-Type-Options", "nosniff")
                            .cacheControl(
                                    CacheControl.maxAge(1, TimeUnit.HOURS)
                            )
                            .body(bytes);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}