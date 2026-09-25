/*
 * Copyright 2026 Vladyslav Livandovskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m000gg.billing.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
@Validated
public class LegalController {

    @Value("${unitbilling.legal.name}")
    private String legalName;

    @Value("${unitbilling.legal.address}")
    private String legalAddress;

    @Value("${unitbilling.legal.city}")
    private String legalCity;

    @Value("${unitbilling.legal.country}")
    private String legalCountry;

    @Value("${unitbilling.legal.email}")
    private String legalEmail;

    @Value("${unitbilling.legal.phone}")
    private String legalPhone;

    @GetMapping("/privacy")
    public String showPrivacy(Model model) {
        addLegalAttributes(model);
        return "privacy";
    }

    @GetMapping("/impressum")
    public String showImpressum(Model model) {
        addLegalAttributes(model);
        return "impressum";
    }

    private void addLegalAttributes(Model model) {
        model.addAttribute("legalName", legalName);
        model.addAttribute("legalAddress", legalAddress);
        model.addAttribute("legalCity", legalCity);
        model.addAttribute("legalCountry", legalCountry);
        model.addAttribute("legalEmail", legalEmail);
        model.addAttribute("legalPhone", legalPhone);
    }
}
