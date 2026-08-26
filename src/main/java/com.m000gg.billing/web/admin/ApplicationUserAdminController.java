package com.m000gg.billing.web.admin;

import com.m000gg.billing.ledger.EntryType;
import com.m000gg.billing.ledger.LedgerEntryAdminViewModel;
import com.m000gg.billing.ledger.LedgerService;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserEditDto;
import com.m000gg.billing.subscribers.ApplicationUserRegisterDto;
import com.m000gg.billing.subscribers.ApplicationUserManagementService;
import com.m000gg.billing.subscribers.exception.EmailAlreadyExistsException;
import com.m000gg.billing.subscribers.exception.EmailAlreadyTakenException;
import com.m000gg.billing.subscribers.exception.UserAlreadyDeletedException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class ApplicationUserAdminController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationUserAdminController.class);

    @Autowired
    private ApplicationUserManagementService applicationUserRegistrationService;

    @Autowired
    private LedgerService ledgerService;

    @GetMapping("/")
    public String usersList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        Page<ApplicationUser> usersPage =  applicationUserRegistrationService.search(search, pageable);
        model.addAttribute("usersPage", usersPage);
        model.addAttribute("search", search);
        return "admin/users";
    }

    @GetMapping("/registration")
    public String showRegistrationForm(Model model){
        model.addAttribute("registerDto", new ApplicationUserRegisterDto());
        return "admin/user-registration";
    }

    @PostMapping("/registration")
    public String registerNewUser(Model model, @Valid @ModelAttribute("registerDto") ApplicationUserRegisterDto applicationUserRegisterDto, BindingResult result) {
        if (result.hasErrors()) {
            return "admin/user-registration";
        }
        try {
            String password = applicationUserRegistrationService.createNewApplicationUser(applicationUserRegisterDto);
            model.addAttribute("generatedPassword", password);
            model.addAttribute("success", true);
            model.addAttribute("registerDto", new ApplicationUserRegisterDto());
        } catch (EmailAlreadyExistsException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to register new application user", ex);
            model.addAttribute("error", "Unexpected error occurred, please try again");
        }
        return "admin/user-registration";
    }

    @GetMapping("/profile/{id}")
    public String getUserProfile(@PathVariable UUID id, Model model) {
        ApplicationUser user = applicationUserRegistrationService.findApplicationUserById(id);
        List<LedgerEntryAdminViewModel> recentLedgerEntries = ledgerService.getUserLastFiveLedgerEntriesForAdmin(user);
        model.addAttribute("recentLedgerEntries", recentLedgerEntries);
        model.addAttribute("user", user);
        return "admin/user-profile";
    }

    @GetMapping("/profile/update/{id}")
    public String showEditForm(@PathVariable UUID id, Model model) {
        ApplicationUserEditDto userDto = applicationUserRegistrationService.findApplicationUserDtoById(id);
        model.addAttribute("user", userDto);
        model.addAttribute("userId", id);
        return "admin/edit-user";
    }

    @PostMapping("/profile/update/{id}")
    public String editUser(@PathVariable UUID id, @Valid @ModelAttribute("user") ApplicationUserEditDto applicationUserEditDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "admin/edit-user";
        }
        try {
            applicationUserRegistrationService.editApplicationUserProfile(id, applicationUserEditDto);
        } catch (EmailAlreadyTakenException e) {
            bindingResult.rejectValue("email", "email.taken", e.getMessage());
            model.addAttribute("userId", id);
            return "admin/edit-user";
        }
        return "redirect:/admin/users/profile/" + id;
    }

    @PostMapping("/profile/delete/{id}")
    public String deleteUser(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            applicationUserRegistrationService.deleteApplicationUserProfile(id);
        } catch (UserAlreadyDeletedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/profile/" + id;
        }
        return "redirect:/admin/users/";
    }

    @GetMapping("/profile/{id}/ledger")
    public String getUserTransactions(@PathVariable UUID id,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) EntryType type,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      Model model) {
        ApplicationUser user = applicationUserRegistrationService.findApplicationUserById(id);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Instant dateFrom = date != null ? date.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant dateTo = date != null ? date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1) : null;

        Page<LedgerEntryAdminViewModel> ledgerEntriesPage = ledgerService.searchForAdmin(user.getId(), search, type, dateFrom, dateTo, pageable);

        model.addAttribute("user", user);
        model.addAttribute("ledgerEntriesPage", ledgerEntriesPage);
        model.addAttribute("search", search);
        model.addAttribute("type", type);
        model.addAttribute("date", date);

        return "admin/user-transactions";
    }
}
