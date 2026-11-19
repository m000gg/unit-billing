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

import com.m000gg.billing.identity.Admin;
import com.m000gg.billing.identity.AdminRepository;
import com.m000gg.billing.plugin.PluginArea;
import com.m000gg.billing.plugin.PluginManager;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserRepository;
import com.m000gg.plugin.api.ui.PluginPageRequest;
import com.m000gg.plugin.api.ui.PluginPageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PluginPageController {

    private final PluginManager pluginManager;

    private final ApplicationUserRepository applicationUserRepository;
    private final AdminRepository adminRepository;

    @RequestMapping(
            value = {
                    "/admin/plugins/{pluginId}",
                    "/admin/plugins/{pluginId}/**",
                    "/client/plugins/{pluginId}",
                    "/client/plugins/{pluginId}/**"
            },
            method = {
                    RequestMethod.GET,
                    RequestMethod.POST
            }
    )
    public String handlePluginPage(
            @PathVariable String pluginId,
            HttpServletRequest request,
            Authentication authentication,
            Locale locale,
            Model model
    ) {

        // Determine plugin area.
        boolean isAdmin = request.getRequestURI()
                .startsWith(request.getContextPath() + "/admin/");

        PluginArea area = isAdmin
                ? PluginArea.ADMIN
                : PluginArea.CLIENT;

        String baseUri = (isAdmin
                ? "/admin/plugins/"
                : "/client/plugins/") + pluginId;

        // Extract the path inside the plugin namespace.
        String requestPath = request.getRequestURI()
                .substring(request.getContextPath().length());

        String path = requestPath.substring(baseUri.length());

        if (path.isEmpty()) {
            path = "/";
        }

        UUID userId = null;

        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {

            String email = authentication.getName();

            if (isAdmin) {
                userId = adminRepository.findByEmail(email)
                        .map(Admin::getId)
                        .orElse(null);
            } else {
                userId = applicationUserRepository.findByEmail(email)
                        .map(ApplicationUser::getId)
                        .orElse(null);
            }
        }

        Map<String, List<String>> parameters = request.getParameterMap()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Arrays.asList(entry.getValue())
                ));


        CsrfToken csrf = (CsrfToken) request.getAttribute(
                CsrfToken.class.getName()
        );

        String csrfParamName = csrf != null
                ? csrf.getParameterName()
                : "_csrf";

        String csrfTokenValue = csrf != null
                ? csrf.getToken()
                : null;

        PluginPageRequest pageRequest = new PluginPageRequest(
                request.getMethod(),
                path,
                parameters,
                userId,
                locale.getLanguage(),
                csrfParamName,
                csrfTokenValue,
                "/plugin-assets/" + pluginId
        );


        PluginPageResponse response = pluginManager.renderPage(
                pluginId,
                area,
                pageRequest
        );


        if (response instanceof PluginPageResponse.Html html) {

            model.addAttribute("pluginTitle", html.title());
            model.addAttribute("pluginBody", html.body());
            model.addAttribute("pluginCss", html.stylesheets());
            model.addAttribute("pluginJs", html.scripts());

            return isAdmin
                    ? "layout/admin-plugin-layout"
                    : "layout/client-plugin-layout";
        }


        if (response instanceof PluginPageResponse.Redirect redirect) {
            return "redirect:" + baseUri + redirect.path();
        }


        if (response instanceof PluginPageResponse.Failure failure) {

            if (failure.status() == PluginPageResponse.Status.FORBIDDEN) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access denied by plugin"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Page not found in plugin"
            );
        }


        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unknown plugin response format"
        );
    }
}