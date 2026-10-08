package com.m000gg.billing.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping("/")
    public String handleRootRequest(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()){
            return "redirect:/login";
        }

        for (GrantedAuthority grantedAuthority: authentication.getAuthorities()){
            if (grantedAuthority.getAuthority().equals("ROLE_ADMIN")) {
                return "redirect:/admin/";
            } else if (grantedAuthority.getAuthority().equals("ROLE_USER")) {
                return "redirect:/client/";
            }
        }
        return "redirect:/login";
    }
}
