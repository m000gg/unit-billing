package com.m000gg.billing.web;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/client/"})
    public String indexClient() {
        return "client/index";
    }

    @GetMapping({"/admin/"})
    public String indexAdmin() {
        return "admin/index";
    }
}
