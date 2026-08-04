package com.m000gg.billing.subscribers;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        String result = applicationUserManagementService.createNewApplicationUser(applicationUserRegisterDto);

        assertEquals("MyTestPassword123", result);
        verify(applicationUserRepository).save(any(ApplicationUser.class));

    }

    @Test
    public void registerApplicationUser_failed_EmailAlreadyExistsException() {
        when(applicationUserRepository.existsByEmail("test123@example.com")).thenReturn(true);
        ApplicationUserRegisterDto applicationUserRegisterDto = new ApplicationUserRegisterDto();
        applicationUserRegisterDto.setEmail("test123@example.com");

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
        when(applicationUserRepository.save(any(ApplicationUser.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key error"));
        assertThatThrownBy(() -> applicationUserManagementService.createNewApplicationUser(applicationUserRegisterDto))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }
}
