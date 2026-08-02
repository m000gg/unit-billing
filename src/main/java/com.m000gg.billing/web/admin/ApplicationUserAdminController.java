package com.m000gg.billing.web.admin;


import com.m000gg.billing.subscribers.ApplicationUserRegisterDto;
import com.m000gg.billing.subscribers.ApplicationUserRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/users")
public class ApplicationUserAdminController {

    @Autowired
    private ApplicationUserRegistrationService applicationUserRegistrationService;

    @GetMapping("/")
    public String showAllApplicationUsers(){
        return "admin/users";
    }


    @GetMapping("/registration")
    public String registerNewApplicationUser(Model model){
        model.addAttribute("registerDto", new ApplicationUserRegisterDto());
        return "admin/user-registration";
    }
}
