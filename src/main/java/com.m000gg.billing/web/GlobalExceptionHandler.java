package com.m000gg.billing.web;

import com.m000gg.billing.subscribers.exception.ApplicationUserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ApplicationUserNotFoundException.class)
    public String handleUserNotFound(ApplicationUserNotFoundException ex,
                                     RedirectAttributes redirectAttributes,
                                     Locale locale) {
        String message = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessage(), locale);
        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:/admin/users/";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception ex, Model model, Locale locale, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        String message = messageSource.getMessage("errors.common.unexpected", null,
                "An unexpected error occurred. Please try again later.", locale);
        model.addAttribute("errorMessage", message);
        model.addAttribute("isAdmin", request.getRequestURI().startsWith("/admin"));
        return "error/500";
    }
}