package com.m000gg.billing.web.admin;

import com.m000gg.billing.settings.BillingSetupDto;
import com.m000gg.billing.settings.SystemSettingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Locale;

@Controller
public class AdminSetupController {
    private static final Logger log = LoggerFactory.getLogger(AdminSetupController.class);

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private SystemSettingService systemSettingService;

    @GetMapping("/admin/billing-setup")
    public String showBillingSetupPage(Model model){
        BillingSetupDto billingSetupDto = new BillingSetupDto();
        model.addAttribute("billingSetupDto", billingSetupDto);
        return "admin/billing-setup";
    }

    @PostMapping("/admin/billing-setup")
    public String setupBillingConfiguration(Model model,
                                            @Valid @ModelAttribute("billingSetupDto") BillingSetupDto billingSetupDto,
                                            BindingResult result,
                                            Locale locale) {
        if (result.hasErrors()) {
            return "admin/billing-setup";
        }
        try {
            systemSettingService.saveInitialSetup(billingSetupDto);
        } catch (Exception ex) {
            log.error("Failed to setup billing system", ex);
            String message = messageSource.getMessage("errors.common.unexpected", null,
                    "Unexpected error occurred, please try again", locale);
            model.addAttribute("error", message);
            return "admin/billing-setup";
        }
        return "redirect:/admin/";
    }
}
