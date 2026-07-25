package com.m000gg.billing.web.client;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/client/"})
    public String index() {
        return "client/index";
    }
}
