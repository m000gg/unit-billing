package com.m000gg.billing.subscribers;

import com.m000gg.billing.settings.exception.InvalidCurrencyException;
import com.m000gg.billing.subscribers.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationUserCRUDServiceTest {

    @Mock
    private ApplicationUserRepository applicationUserRepository;

    @InjectMocks
    private ApplicationUserManagementService applicationUserManagementService;

    @Mock
    private CustomPasswordGenerator customPasswordGenerator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationUserMapper applicationUserMapper;

    @Test
    public void registerApplicationUser_successful() {
        when(applicationUserRepository.existsByEmail("test123@example.com")).thenReturn(false);
        when(customPasswordGenerator.generatePassayPassword()).thenReturn("MyTestPassword123");
        when(passwordEncoder.encode("MyTestPassword123")).thenReturn("hashed_MyTestPassword123");

        ApplicationUserRegisterDto applicationUserRegisterDto = new ApplicationUserRegisterDto();
        applicationUserRegisterDto.setEmail("test123@example.com");
        applicationUserRegisterDto.setUserCurrency("USD");
        String result = applicationUserManagementService.createNewApplicationUser(applicationUserRegisterDto);

        assertEquals("MyTestPassword123", result);
        verify(applicationUserRepository).save(any(ApplicationUser.class));
    }

    @Test
    public void registerApplicationUser_failed_EmailAlreadyExistsException() {
        when(applicationUserRepository.existsByEmail("test123@example.com")).thenReturn(true);
        ApplicationUserRegisterDto applicationUserRegisterDto = new ApplicationUserRegisterDto();
        applicationUserRegisterDto.setEmail("test123@example.com");
        applicationUserRegisterDto.setUserCurrency("USD");

        assertThatThrownBy(() -> applicationUserManagementService.createNewApplicationUser(applicationUserRegisterDto))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    public void registerApplicationUser_failed_DataIntegrityViolationException() {
        when(applicationUserRepository.existsByEmail("test123@example.com")).thenReturn(false);
        when(customPasswordGenerator.generatePassayPassword()).thenReturn("MyTestPassword123");
        when(passwordEncoder.encode("MyTestPassword123")).thenReturn("hashed_MyTestPassword123");
        ApplicationUserRegisterDto applicationUserRegisterDto = new ApplicationUserRegisterDto();
        applicationUserRegisterDto.setEmail("test123@example.com");
        applicationUserRegisterDto.setUserCurrency("USD");
        when(applicationUserRepository.save(any(ApplicationUser.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key error"));
        assertThatThrownBy(() -> applicationUserManagementService.createNewApplicationUser(applicationUserRegisterDto))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    public void registerApplicationUser_missingCurrency_throwsInvalidCurrencyException() {
        when(applicationUserRepository.existsByEmail("test123@example.com")).thenReturn(false);
        ApplicationUserRegisterDto applicationUserRegisterDto = new ApplicationUserRegisterDto();
        applicationUserRegisterDto.setEmail("test123@example.com");
        assertThatThrownBy(() -> applicationUserManagementService.createNewApplicationUser(applicationUserRegisterDto))
                .isInstanceOf(InvalidCurrencyException.class);
        verify(applicationUserRepository, never()).save(any());
    }

    @Test
    public void registerApplicationUser_invalidCurrencyCode_throwsInvalidCurrencyException() {
        when(applicationUserRepository.existsByEmail("test123@example.com")).thenReturn(false);
        ApplicationUserRegisterDto applicationUserRegisterDto = new ApplicationUserRegisterDto();
        applicationUserRegisterDto.setEmail("test123@example.com");
        applicationUserRegisterDto.setUserCurrency("ZZZ");
        assertThatThrownBy(() -> applicationUserManagementService.createNewApplicationUser(applicationUserRegisterDto))
                .isInstanceOf(InvalidCurrencyException.class);
        verify(applicationUserRepository, never()).save(any());
    }
}
