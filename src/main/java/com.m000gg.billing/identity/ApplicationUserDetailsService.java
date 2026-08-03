package com.m000gg.billing.identity;

import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ApplicationUserDetailsService implements UserDetailsService {

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {


        Admin admin = adminRepository.findByEmail(email).orElse(null);
        if (admin != null) {
            return org.springframework.security.core.userdetails.User
                    .withUsername(admin.getEmail())
                    .password(admin.getPassword())
                    .roles("ADMIN")
                    .build();
        }

        ApplicationUser appUser = applicationUserRepository.findByEmail(email).orElse(null);
        if (appUser == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        boolean deleted = Boolean.TRUE.equals(appUser.getDeleted());

        return org.springframework.security.core.userdetails.User
                .withUsername(appUser.getEmail())
                .password(appUser.getPassword())
                .disabled(deleted)
                .roles("USER")
                .build();
    }
}
