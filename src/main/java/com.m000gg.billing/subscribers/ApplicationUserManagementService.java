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


import com.m000gg.billing.settings.exception.InvalidCurrencyException;
import com.m000gg.billing.subscribers.exception.ApplicationUserNotFoundException;
import com.m000gg.billing.subscribers.exception.EmailAlreadyExistsException;
import com.m000gg.billing.subscribers.exception.EmailAlreadyTakenException;
import com.m000gg.billing.subscribers.exception.UserAlreadyDeletedException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Currency;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApplicationUserManagementService {

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationUserMapper applicationUserMapper;

    @Autowired
    private CustomPasswordGenerator customPasswordGenerator;

    private static final Set<String> VALID_CURRENCIES = Currency.getAvailableCurrencies()
            .stream()
            .map(Currency::getCurrencyCode)
            .collect(Collectors.toSet());

    @Transactional
    public String createNewApplicationUser(ApplicationUserRegisterDto applicationUserRegisterDto) {

        ApplicationUser newApplicationUser = new ApplicationUser();

        String email = applicationUserRegisterDto.getEmail();
        if (applicationUserRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        String userCurrency = applicationUserRegisterDto.getUserCurrency();
        String normalizedCurrency = userCurrency == null ? null : userCurrency.toUpperCase(java.util.Locale.ROOT);
        if (normalizedCurrency == null || normalizedCurrency.isBlank() || !VALID_CURRENCIES.contains(normalizedCurrency)) {
            throw new InvalidCurrencyException(userCurrency);
        }
        applicationUserRegisterDto.setUserCurrency(normalizedCurrency);
        String generatedPassword = customPasswordGenerator.generatePassayPassword();
        String encodedPassword = passwordEncoder.encode(generatedPassword);
        applicationUserMapper.registerUserFromDto(newApplicationUser, applicationUserRegisterDto, encodedPassword);

        try {
            applicationUserRepository.save(newApplicationUser);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(email);
        }

        return generatedPassword;
    }

    public Page<ApplicationUser> search(String search, Pageable pageable) {
        return applicationUserRepository.search(search, pageable);
    }

    public ApplicationUser findApplicationUserById(UUID id) {
        return applicationUserRepository.findById(id)
                .orElseThrow(() -> new ApplicationUserNotFoundException(id));
    }

    public ApplicationUserEditDto findApplicationUserDtoById(UUID id) {
        return applicationUserMapper.toDto(findApplicationUserById(id));
    }

    @Transactional
    public void editApplicationUserProfile(UUID id, ApplicationUserEditDto dataToChange) {
        ApplicationUser user = findApplicationUserById(id);

        if (!user.getEmail().equals(dataToChange.getEmail())
                && applicationUserRepository.existsByEmail(dataToChange.getEmail())) {
            throw new EmailAlreadyTakenException(dataToChange.getEmail());
        }

        applicationUserMapper.updateEntityFromDto(user, dataToChange);

        applicationUserRepository.save(user);
    }

    @Transactional
    public void deleteApplicationUserProfile(UUID id) {
        ApplicationUser applicationUser = findApplicationUserById(id);
        if (!applicationUser.getDeleted()) {
            applicationUser.setDeleted(true);
            applicationUserRepository.save(applicationUser);
        } else {
            throw new UserAlreadyDeletedException("This user with id: " + id + " is already deleted.");
        }
    }

    public Optional<ApplicationUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        String email = authentication.getName();
        return applicationUserRepository.findByEmail(email)
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()));
    }

    public AccountOverviewViewModel getUserInformationForMainPage(ApplicationUser applicationUser) {
        AccountOverviewViewModel accountOverviewViewModel = new AccountOverviewViewModel();
        return applicationUserMapper.accountViewModelFromUser(applicationUser, accountOverviewViewModel);
    }
}