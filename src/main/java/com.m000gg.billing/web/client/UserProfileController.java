package com.m000gg.billing.web.client;

import com.m000gg.billing.ledger.EntryType;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
                                  @RequestParam(required = false) EntryType type,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  Model model) {

        Optional<ApplicationUser> userOptional = applicationUserManagementService.getCurrentUser();
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        ApplicationUser user = userOptional.get();
        AccountOverviewViewModel accountOverviewViewModel = applicationUserManagementService.getUserInformationForMainPage(user);

        Instant dateFrom = date != null ? date.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant dateTo = date != null ? date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1) : null;

        Page<LedgerEntryUserViewModel> ledgerEntriesPage = ledgerService.search(user.getId(), search, type, dateFrom, dateTo, pageable);

        model.addAttribute("ledgerEntriesPage", ledgerEntriesPage);
        model.addAttribute("search", search);
        model.addAttribute("type", type);
        model.addAttribute("date", date);
        model.addAttribute("userProfileViewModel", accountOverviewViewModel);
        return "client/profile";
    }
}
