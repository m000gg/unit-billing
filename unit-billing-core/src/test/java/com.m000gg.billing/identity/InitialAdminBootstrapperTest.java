package com.m000gg.billing.identity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapperTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private InitialAdminBootstrapper bootstrapper;

    @Test
    void skipsCreation_WhenAdminAlreadyExists() {
        when(adminRepository.count()).thenReturn(1L);
        bootstrapper.initializeFirstAdmin();
        verify(passwordEncoder, never()).encode(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    void skipsCreation_WhenCredentialsAreMissing() {
        when(adminRepository.count()).thenReturn(0L);
        ReflectionTestUtils.setField(bootstrapper, "initialAdminEmail", "admin@domain.com");
        ReflectionTestUtils.setField(bootstrapper, "initialAdminPassword", "");
        bootstrapper.initializeFirstAdmin();
        verify(adminRepository, never()).save(any());
    }

    @Test
    void createsAdmin_WhenDatabaseIsEmpty_AndCredentialsAreValid() {
        when(adminRepository.count()).thenReturn(0L);
        ReflectionTestUtils.setField(bootstrapper, "initialAdminEmail", "admin@domain.com");
        ReflectionTestUtils.setField(bootstrapper, "initialAdminPassword", "secret123");
        when(passwordEncoder.encode("secret123")).thenReturn("hashed_secret");

        bootstrapper.initializeFirstAdmin();
        ArgumentCaptor<Admin> adminCaptor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository, times(1)).save(adminCaptor.capture());

        Admin savedAdmin = adminCaptor.getValue();
        assertEquals("admin@domain.com", savedAdmin.getEmail());
        assertEquals("hashed_secret", savedAdmin.getPassword());
    }
}
