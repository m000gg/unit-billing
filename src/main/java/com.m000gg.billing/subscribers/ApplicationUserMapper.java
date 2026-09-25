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

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ApplicationUserMapper {

    public void updateEntityFromDto(ApplicationUser entity, ApplicationUserEditDto dto) {
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setCountry(dto.getCountry());
        entity.setCity(dto.getCity());
        entity.setRegion(dto.getRegion());
        entity.setStreet(dto.getStreet());
        entity.setHouseNumber(dto.getHouseNumber());
        entity.setApartment(dto.getApartment());
        entity.setPostalCode(dto.getPostalCode());
    }

    public ApplicationUserEditDto toDto(ApplicationUser entity) {
        ApplicationUserEditDto dto = new ApplicationUserEditDto();
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setCountry(entity.getCountry());
        dto.setCity(entity.getCity());
        dto.setRegion(entity.getRegion());
        dto.setStreet(entity.getStreet());
        dto.setHouseNumber(entity.getHouseNumber());
        dto.setApartment(entity.getApartment());
        dto.setPostalCode(entity.getPostalCode());
        dto.setUserCurrency(entity.getUserCurrency());
        return dto;
    }

    public void registerUserFromDto(ApplicationUser newApplicationUser, ApplicationUserRegisterDto applicationUserRegisterDto, String encodedPassword) {
        newApplicationUser.setFirstName(applicationUserRegisterDto.getFirstName());
        newApplicationUser.setLastName(applicationUserRegisterDto.getLastName());
        newApplicationUser.setEmail(applicationUserRegisterDto.getEmail());
        newApplicationUser.setPhone(applicationUserRegisterDto.getPhone());
        newApplicationUser.setCountry(applicationUserRegisterDto.getCountry());
        newApplicationUser.setCity(applicationUserRegisterDto.getCity());
        newApplicationUser.setRegion(applicationUserRegisterDto.getRegion());
        newApplicationUser.setStreet(applicationUserRegisterDto.getStreet());
        newApplicationUser.setHouseNumber(applicationUserRegisterDto.getHouseNumber());
        newApplicationUser.setApartment(applicationUserRegisterDto.getApartment());
        newApplicationUser.setPostalCode(applicationUserRegisterDto.getPostalCode());
        newApplicationUser.setBalance(BigDecimal.ZERO);
        newApplicationUser.setPassword(encodedPassword);
        newApplicationUser.setUserCurrency(applicationUserRegisterDto.getUserCurrency());
    }

    public AccountOverviewViewModel accountViewModelFromUser(ApplicationUser currentUser, AccountOverviewViewModel accountOverviewViewModel) {
        accountOverviewViewModel.setBalance(currentUser.getBalance());
        accountOverviewViewModel.setFirstName(currentUser.getFirstName());
        accountOverviewViewModel.setLastName(currentUser.getLastName());
        accountOverviewViewModel.setEmail(currentUser.getEmail());
        accountOverviewViewModel.setCity(currentUser.getCity());
        accountOverviewViewModel.setCountry(currentUser.getCountry());
        accountOverviewViewModel.setRegion(currentUser.getRegion());
        accountOverviewViewModel.setStreet(currentUser.getStreet());
        accountOverviewViewModel.setCreatedAt(currentUser.getCreatedAt());
        accountOverviewViewModel.setHouseNumber(currentUser.getHouseNumber());
        accountOverviewViewModel.setApartment(currentUser.getApartment());
        accountOverviewViewModel.setPostalCode(currentUser.getPostalCode());
        accountOverviewViewModel.setPhone(currentUser.getPhone());
        accountOverviewViewModel.setUserCurrency(currentUser.getUserCurrency());
        return accountOverviewViewModel;
    }
}