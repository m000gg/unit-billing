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
import com.m000gg.billing.subscribers.AccountOverviewViewModel;
import com.m000gg.billing.subscribers.ApplicationUserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class ClientModelAdvice {

    private final PluginManager pluginManager;
    private final ApplicationUserManagementService applicationUserManagementService;

    @ModelAttribute
    public void addClientNavigation(Model model, HttpServletRequest request) {
        model.addAttribute("activePlugins", pluginManager.getClientPluginIds());
        model.addAttribute("pluginBaseUrl", "/client/plugins/");
        model.addAttribute("currentUri", request.getRequestURI());

        applicationUserManagementService.getCurrentUser().ifPresent(user -> {
            AccountOverviewViewModel userViewModel =
                    applicationUserManagementService.getUserInformationForMainPage(user);

            model.addAttribute("userViewModel", userViewModel);
        });
    }
}

