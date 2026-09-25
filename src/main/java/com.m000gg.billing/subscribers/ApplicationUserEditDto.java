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

package com.m000gg.billing.subscribers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public class ApplicationUserEditDto {
    @NotEmpty(message = "{subscribers.validation.firstName.required}")
    private String firstName;

    @NotEmpty(message = "{subscribers.validation.lastName.required}")
    private String lastName;

    @NotEmpty(message = "{subscribers.validation.email.required}")
    @Email(message = "{subscribers.validation.email.invalid}")
    private String email;

    @NotEmpty(message = "{subscribers.validation.phone.required}")
    private String phone;

    @NotEmpty(message = "{subscribers.validation.country.required}")
    private String country;

    @NotEmpty(message = "{subscribers.validation.city.required}")
    private String city;

    private String region;

    @NotEmpty(message = "{subscribers.validation.street.required}")
    private String street;

    @NotEmpty(message = "{subscribers.validation.houseNumber.required}")
    private String houseNumber;

    private String apartment;

    @NotEmpty(message = "{subscribers.validation.postalCode.required}")
    private String postalCode;

    private String userCurrency;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }

    public String getApartment() { return apartment; }
    public void setApartment(String apartment) { this.apartment = apartment; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getUserCurrency() {
        return userCurrency;
    }
    public void setUserCurrency(String userCurrency) {
        this.userCurrency = userCurrency;
    }
}
