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

import com.m000gg.billing.settings.SystemSettingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class ApplicationUserCRUDIntegrationTest {
    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @MockitoBean
    private SystemSettingService systemSettingService;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(systemSettingService.isBaseCurrencyConfigured()).thenReturn(true);
    }

    @AfterEach
    void cleanUp() {
        applicationUserRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsersList_ReturnsValidPageAndModel() throws Exception {
        mockMvc.perform(get("/admin/users/")
                        .param("page", "0")
                        .param("size", "5")
                        .param("search", "Ivan"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("usersPage"))
                .andExpect(model().attributeExists("search"))
                .andExpect(model().attribute("search", "Ivan"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createNewUser_Success_RedirectsToList() throws Exception {
        mockMvc.perform(post("/admin/users/registration")
                        .with(csrf())
                        .param("firstName", "John")
                        .param("lastName", "Pork")
                        .param("email", "new_user@example.com")
                        .param("phone", "+4915123456789")
                        .param("country", "Germany")
                        .param("city", "Chemnitz")
                        .param("street", "Hauptstrasse")
                        .param("houseNumber", "12")
                        .param("postalCode", "09111")
                        .param("userCurrency", "USD"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-registration"))
                .andExpect(model().attribute("success", true))
                .andExpect(model().attributeExists("generatedPassword"));
        boolean userExists = applicationUserRepository.existsByEmail("new_user@example.com");
        assertThat(userExists).isTrue();
    }

    @Test
    void getUsersList_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
