package com.m000gg.billing.web.client;


import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping({"/client/login"})
    public String login(Model model, Authentication authentication) {

        return "client/auth/login";
    }
}
