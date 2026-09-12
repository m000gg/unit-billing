package com.m000gg.billing.web.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientSystemStatusController {

    @GetMapping("/client/billing-not-configured")
    public String billingNotConfiguredPage(){
        return "client/not-configured";
    }
}
