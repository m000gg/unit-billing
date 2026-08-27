package com.m000gg.billing.web.admin;
import com.m000gg.billing.identity.Admin;
import com.m000gg.billing.identity.AdminManagementService;
import com.m000gg.billing.ledger.BillRequestDto;
import com.m000gg.billing.ledger.LedgerService;
import com.m000gg.billing.ledger.RefundRequestDto;
import com.m000gg.billing.ledger.TopUpRequestDto;
import com.m000gg.billing.ledger.CorrectionRequestDto;
import com.m000gg.billing.ledger.exception.InsufficientBalanceException;
import com.m000gg.billing.ledger.exception.InvalidRefundTargetException;
import com.m000gg.billing.ledger.exception.RefundExceedsOriginalChargeException;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class LedgerEntryAdminController {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private ApplicationUserManagementService applicationUserManagementService;

    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/{id}/topup")
    public String showManualTopUpPage(@PathVariable UUID id, Model model){
        TopUpRequestDto topUpRequestDto = new TopUpRequestDto();
        model.addAttribute("topUpRequest", topUpRequestDto);
        model.addAttribute("user", applicationUserManagementService.findApplicationUserById(id));

        return "admin/topup";
    }

    @PostMapping("/{id}/topup")
    public String addTopUpToUser(@PathVariable UUID id,
                                 @Valid @ModelAttribute("topUpRequest") TopUpRequestDto topUpRequestDto,
                                 BindingResult bindingResult,
                                 Model model,
                                 Locale locale) {
        Optional<Admin> currentAdminOptional = adminManagementService.getCurrentAdmin();
        if (currentAdminOptional.isEmpty()) {
            return "redirect:/login";
        }
        Admin currentAdmin = currentAdminOptional.get();
        ApplicationUser user = applicationUserManagementService.findApplicationUserById(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/topup";
        }
        try {
            ledgerService.applyTopUp(topUpRequestDto, user, currentAdmin);
        } catch (ObjectOptimisticLockingFailureException ex) {
            String message = messageSource.getMessage("errors.common.concurrentUpdate", null, locale);
            model.addAttribute("errorMessage", message);
            model.addAttribute("user", user);
            return "admin/topup";
        }
        return "redirect:/admin/users/profile/" + id;
    }

    @GetMapping("/{id}/bill")
    public String showManualBillPage(@PathVariable UUID id, Model model){
        BillRequestDto billRequestDto = new BillRequestDto();
        model.addAttribute("billRequest", billRequestDto);
        model.addAttribute("user", applicationUserManagementService.findApplicationUserById(id));
        return "admin/bill";
    }

    @PostMapping("/{id}/bill")
    public String issueBillForUser(@PathVariable UUID id,
                                   @Valid @ModelAttribute("billRequest") BillRequestDto billRequestDto,
                                   BindingResult bindingResult,
                                   Model model,
                                   Locale locale) {
        Optional<Admin> currentAdminOptional = adminManagementService.getCurrentAdmin();
        if (currentAdminOptional.isEmpty()) {
            return "redirect:/login";
        }
        Admin currentAdmin = currentAdminOptional.get();
        ApplicationUser user = applicationUserManagementService.findApplicationUserById(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/bill";
        }

        try {
            ledgerService.issueBill(billRequestDto, user, currentAdmin);
        } catch (InsufficientBalanceException ex) {
            String message = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessage(), locale);
            bindingResult.rejectValue("amount", "insufficient.balance", message);
            model.addAttribute("user", user);
            return "admin/bill";
        } catch (ObjectOptimisticLockingFailureException ex) {
            String message = messageSource.getMessage("errors.common.concurrentUpdate", null, locale);
            model.addAttribute("errorMessage", message);
            model.addAttribute("user", user);
            return "admin/bill";
        }
        return "redirect:/admin/users/profile/" + id;
    }

    @GetMapping("/{id}/refund")
    public String showRefundForm(@PathVariable UUID id,@RequestParam(required = false) UUID originalEntryId,Model model){
        RefundRequestDto refundRequestDto = new RefundRequestDto();
        if (originalEntryId != null) {
            refundRequestDto.setOriginalEntryId(originalEntryId);
        }
        model.addAttribute("user", applicationUserManagementService.findApplicationUserById(id));
        model.addAttribute("refundRequest", refundRequestDto);
        model.addAttribute("availableCharges", ledgerService.findRefundableCharges(id));
        return "admin/refund";
    }

    @PostMapping("/{id}/refund")
    public String applyRefundForUser(@PathVariable UUID id,
                                     @Valid @ModelAttribute("refundRequest") RefundRequestDto refundRequestDto,
                                     BindingResult bindingResult,
                                     Model model,
                                     Locale locale) {

        Optional<Admin> currentAdminOptional = adminManagementService.getCurrentAdmin();
        if (currentAdminOptional.isEmpty()) {
            return "redirect:/login";
        }
        Admin currentAdmin = currentAdminOptional.get();

        ApplicationUser user = applicationUserManagementService.findApplicationUserById(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("availableCharges", ledgerService.findRefundableCharges(id));
            return "admin/refund";
        }

        try {
            ledgerService.applyRefund(refundRequestDto, user, currentAdmin);
        } catch (InvalidRefundTargetException ex) {
            String message = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessage(), locale);
            bindingResult.rejectValue("originalEntryId", "invalid.refund.target", message);
            model.addAttribute("user", user);
            model.addAttribute("availableCharges", ledgerService.findRefundableCharges(id));
            return "admin/refund";
        } catch (RefundExceedsOriginalChargeException ex) {
            String message = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessage(), locale);
            bindingResult.rejectValue("amount", "refund.exceeds.charge", message);
            model.addAttribute("user", user);
            model.addAttribute("availableCharges", ledgerService.findRefundableCharges(id));
            return "admin/refund";
        } catch (ObjectOptimisticLockingFailureException ex) {
            String message = messageSource.getMessage("errors.common.concurrentUpdate", null, locale);
            model.addAttribute("errorMessage", message);
            model.addAttribute("user", user);
            model.addAttribute("availableCharges", ledgerService.findRefundableCharges(id));
            return "admin/refund";
        }

        return "redirect:/admin/users/profile/" + id;
    }

    @GetMapping("/{id}/correction")
    public String showCorrectionForm(@PathVariable UUID id,Model model){
        CorrectionRequestDto correctionRequestDto = new CorrectionRequestDto();
        model.addAttribute("user", applicationUserManagementService.findApplicationUserById(id));
        model.addAttribute("correctionRequest", correctionRequestDto);
        return "admin/correction";
    }

    @PostMapping("/{id}/correction")
    public String applyCorrectionForUser(@PathVariable UUID id,
                                         @Valid @ModelAttribute("correctionRequest") CorrectionRequestDto correctionRequestDto,
                                         BindingResult bindingResult,
                                         Model model,
                                         Locale locale) {

        Optional<Admin> currentAdminOptional = adminManagementService.getCurrentAdmin();
        if (currentAdminOptional.isEmpty()) {
            return "redirect:/login";
        }
        Admin currentAdmin = currentAdminOptional.get();

        ApplicationUser user = applicationUserManagementService.findApplicationUserById(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/correction";
        }

        try {
            ledgerService.applyCorrection(correctionRequestDto, user, currentAdmin);
        } catch (InsufficientBalanceException ex) {
            String message = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessage(), locale);
            bindingResult.rejectValue("amount", "insufficient.balance", message);
            model.addAttribute("user", user);
            return "admin/correction";
        } catch (ObjectOptimisticLockingFailureException ex) {
            String message = messageSource.getMessage("errors.common.concurrentUpdate", null, locale);
            model.addAttribute("errorMessage", message);
            model.addAttribute("user", user);
            return "admin/correction";
        }

        return "redirect:/admin/users/profile/" + id;
    }
}