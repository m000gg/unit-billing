package com.m000gg.billing.web.client;

import com.m000gg.billing.ledger.LedgerEntryUserViewModel;
import com.m000gg.billing.ledger.LedgerService;
import com.m000gg.billing.subscribers.AccountOverviewViewModel;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class UserProfileController {

    @Autowired
    private ApplicationUserManagementService applicationUserManagementService;

    @Autowired
    private LedgerService ledgerService;

    @GetMapping("/client/profile")
    public String showUserProfile(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) String search,
                                  Model model){

        Optional<ApplicationUser> userOptional = applicationUserManagementService.getCurrentUser();
        if (userOptional.isPresent()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            ApplicationUser user = userOptional.get();
            AccountOverviewViewModel accountOverviewViewModel = applicationUserManagementService.getUserInformationForMainPage(user);
            Page<LedgerEntryUserViewModel> ledgerEntriesPage = ledgerService.search(user.getId(),search, pageable);
            model.addAttribute("ledgerEntriesPage", ledgerEntriesPage);
            model.addAttribute("search", search);
            model.addAttribute("userProfileViewModel", accountOverviewViewModel);
        } else {
            return "redirect:/login";
        }
        return "client/profile";
    }
}
