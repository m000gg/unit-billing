package com.m000gg.billing.web.client;

import com.m000gg.billing.subscribers.AccountOverviewViewModel;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class UserProfileController {

    @Autowired
    private ApplicationUserManagementService applicationUserManagementService;

    @GetMapping("/client/profile")
    public String showUserProfile(Model model){

        Optional<ApplicationUser> userOptional = applicationUserManagementService.getCurrentUser();
        if (userOptional.isPresent()) {
            ApplicationUser user = userOptional.get();

            AccountOverviewViewModel accountOverviewViewModel = applicationUserManagementService.getUserInformationForMainPage(user);
            model.addAttribute("userProfileViewModel", accountOverviewViewModel);
        } else {
            return "redirect:/login";
        }
        return "client/profile";
    }
}
