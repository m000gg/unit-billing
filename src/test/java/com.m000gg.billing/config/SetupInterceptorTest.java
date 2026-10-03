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

package com.m000gg.billing.config;

import com.m000gg.billing.configs.SetupInterceptor;
import com.m000gg.billing.settings.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetupInterceptorTest {

    @Mock
    private SystemSettingService systemSettingService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SetupInterceptor setupInterceptor;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldAllowAccess_WhenBaseCurrencyIsConfigured() throws Exception {
        when(systemSettingService.isBaseCurrencyConfigured()).thenReturn(true);
        boolean result = setupInterceptor.preHandle(request, response, new Object());
        assertTrue(result);
        verifyNoInteractions(request, response, securityContext);
    }

    @Test
    void shouldAllowAccess_WhenNotAuthenticated() throws Exception {
        when(systemSettingService.isBaseCurrencyConfigured()).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(null);

        boolean result = setupInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    void shouldRedirectAdmin_ToSetupPage_WhenBaseCurrencyNotConfigured() throws Exception {
        when(systemSettingService.isBaseCurrencyConfigured()).thenReturn(false);
        when(request.getRequestURI()).thenReturn("/admin/dashboard");
        mockUserRole("ROLE_ADMIN");

        boolean result = setupInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).sendRedirect("/admin/billing-setup");
    }

    @Test
    void shouldAllowAdminAccess_ToSetupPage_WhenBaseCurrencyNotConfigured() throws Exception {
        when(systemSettingService.isBaseCurrencyConfigured()).thenReturn(false);
        when(request.getRequestURI()).thenReturn("/admin/billing-setup");
        mockUserRole("ROLE_ADMIN");

        boolean result = setupInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void shouldRedirectUser_ToNotConfiguredPage_WhenBaseCurrencyNotConfigured() throws Exception {
        when(systemSettingService.isBaseCurrencyConfigured()).thenReturn(false);
        when(request.getRequestURI()).thenReturn("/client/profile");
        mockUserRole("ROLE_USER");

        boolean result = setupInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).sendRedirect("/client/billing-not-configured");
    }

    @Test
    void shouldAllowUserAccess_ToNotConfiguredPage_WhenBaseCurrencyNotConfigured() throws Exception {
        when(systemSettingService.isBaseCurrencyConfigured()).thenReturn(false);
        when(request.getRequestURI()).thenReturn("/client/billing-not-configured");
        mockUserRole("ROLE_USER");

        boolean result = setupInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response, never()).sendRedirect(anyString());
    }

    private void mockUserRole(String role) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        GrantedAuthority authority = new SimpleGrantedAuthority(role);
        doReturn(Collections.singletonList(authority)).when(authentication).getAuthorities();
    }
}
