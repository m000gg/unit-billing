package com.m000gg.billing.web.admin;

import com.m000gg.billing.ledger.BillRequestDto;
import com.m000gg.billing.ledger.LedgerService;
import com.m000gg.billing.ledger.TopUpRequestDto;
import com.m000gg.billing.ledger.exception.InsufficientBalanceException;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class LedgerEntryAdminController {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private ApplicationUserManagementService applicationUserManagementService;

    @GetMapping("/admin/users/{id}/topup")
    public String showManualTopUpPage(@PathVariable UUID id, Model model){
        TopUpRequestDto topUpRequestDto = new TopUpRequestDto();
        model.addAttribute("topUpRequest", topUpRequestDto);
        model.addAttribute("user", applicationUserManagementService.findApplicationUserById(id));

        return "admin/topup";
    }

    @PostMapping("/admin/users/{id}/topup")
    public String addTopUpToUser(@PathVariable UUID id,
                                 @Valid @ModelAttribute("topUpRequest") TopUpRequestDto topUpRequestDto,
                                 BindingResult bindingResult,
                                 Model model) {

        ApplicationUser user = applicationUserManagementService.findApplicationUserById(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/topup";
        }
        ledgerService.applyTopUp(topUpRequestDto, id);
        return "redirect:/admin/users/profile/" + id;
    }

    @GetMapping("/admin/users/{id}/bill")
    public String showManualBillPage(@PathVariable UUID id, Model model){
        BillRequestDto billRequestDto = new BillRequestDto();
        model.addAttribute("billRequest", billRequestDto);
        model.addAttribute("user", applicationUserManagementService.findApplicationUserById(id));
        return "admin/bill";
    }

    @PostMapping("/admin/users/{id}/bill")
    public String issueBillForUser(@PathVariable UUID id,
                             @Valid @ModelAttribute("billRequest") BillRequestDto billRequestDto,
                             BindingResult bindingResult,
                             Model model){
        ApplicationUser user = applicationUserManagementService.findApplicationUserById(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/bill";
        }

        try {
            ledgerService.issueBill(billRequestDto,id);
        } catch (InsufficientBalanceException ex) {
            bindingResult.rejectValue("amount", "insufficient.balance",
                    "Amount exceeds available balance ($" + user.getBalance() + ")");
            model.addAttribute("user", user);
            return "admin/bill";
        }
        return "redirect:/admin/users/profile/" + id;

    }
}
