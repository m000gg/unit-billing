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

package com.m000gg.billing.identity;



import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private ApplicationUserRepository applicationUserRepository;

    @Mock
    private AdminRepository adminRepository;


    @InjectMocks
    private ApplicationUserDetailsService applicationUserDetailsService;


    @Test
    public void userDetails_successful() {
        ApplicationUser appUser = new ApplicationUser();
        appUser.setEmail("ivan@test.com");
        appUser.setPassword("encodedPassword");

        when(applicationUserRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(appUser));

        UserDetails result = applicationUserDetailsService.loadUserByUsername("ivan@test.com");

        assertThat(result.getUsername()).isEqualTo("ivan@test.com");
    }

    @Test
    public void userDetails_failed_shouldThrowException() {

        when(applicationUserRepository.findByEmail("aaa@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationUserDetailsService.loadUserByUsername("aaa@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }


    @Test
    public void adminDetails_successful() {
        Admin admin = new Admin();
        admin.setEmail("admin@test.com");
        admin.setPassword("encodedPassword");

        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        UserDetails result = applicationUserDetailsService.loadUserByUsername("admin@test.com");
        assertThat(result.getUsername()).isEqualTo("admin@test.com");
    }

    @Test
    public void adminDetails_failed_shouldThrowException() {
        when(adminRepository.findByEmail("aaa@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationUserDetailsService.loadUserByUsername("aaa@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

}
