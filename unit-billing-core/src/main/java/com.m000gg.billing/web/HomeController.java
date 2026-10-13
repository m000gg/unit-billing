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

import com.m000gg.billing.ledger.LedgerEntryUserViewModel;
import com.m000gg.billing.ledger.LedgerService;
import com.m000gg.billing.subscribers.AccountOverviewViewModel;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private ApplicationUserManagementService applicationUserManagementService;

    @Autowired
    private LedgerService ledgerService;

    @GetMapping({"/client/"})
    public String indexClient(Model model) {

        Optional<ApplicationUser> userOptional = applicationUserManagementService.getCurrentUser();
        if (userOptional.isPresent()) {
            ApplicationUser user = userOptional.get();
            List<LedgerEntryUserViewModel> ledgerEntryUserViewModelList = ledgerService.getUserLastFiveLedgerEntries(user);
            AccountOverviewViewModel accountOverviewViewModel = applicationUserManagementService.getUserInformationForMainPage(user);
            model.addAttribute("userViewModel", accountOverviewViewModel);
            model.addAttribute("ledgerEntryUserViewModelList", ledgerEntryUserViewModelList);
        } else {
            return "redirect:/login";
        }
        return "client/index";
    }

    @GetMapping({"/admin/"})
    public String indexAdmin() {
        return "admin/index";
    }
}
