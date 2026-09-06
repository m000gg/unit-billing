package com.m000gg.billing.configs;

import com.m000gg.billing.settings.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SetupInterceptor  implements HandlerInterceptor {
    @Autowired
    private SystemSettingService systemSettingService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (systemSettingService.isBaseCurrencyConfigured()){
            return true;
        }

        String requestURI = request.getRequestURI();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()){
            return true;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin){
            if (requestURI.startsWith("/admin/billing-setup")){
                return true;
            }
            else{
                response.sendRedirect("/admin/billing-setup");
                return false;
            }
        }

        boolean isUser = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

        if (isUser){
            if (requestURI.startsWith("/client/billing-not-configured")){
                return true;
            }
            else{
                response.sendRedirect("/client/billing-not-configured");
                return false;
            }
        }
        return true;
    }
}
