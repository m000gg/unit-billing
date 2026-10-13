package com.m000gg.billing.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialAdminBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrapper.class);

    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.security.initial-admin.email:}")
    private String initialAdminEmail;

    @Value("${app.security.initial-admin.password:}")
    private String initialAdminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeFirstAdmin() {
        if (adminRepository.count() > 0) {
            log.info("Admins table is not empty. Skipping initial admin creation.");
            return;
        }

        if (initialAdminEmail == null || initialAdminEmail.isBlank() ||
                initialAdminPassword == null || initialAdminPassword.isBlank()) {
            log.warn("Initial admin credentials are not fully set in environment variables. Cannot create bootstrap admin!");
            return;
        }

        log.info("Admins table is empty. Provisioning initial admin: {}", initialAdminEmail);

        Admin admin = new Admin();
        admin.setEmail(initialAdminEmail);

        admin.setPassword(passwordEncoder.encode(initialAdminPassword));

        adminRepository.save(admin);
        log.info("Initial admin successfully provisioned.");
    }
}
