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
