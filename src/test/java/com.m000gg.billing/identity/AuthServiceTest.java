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

}
