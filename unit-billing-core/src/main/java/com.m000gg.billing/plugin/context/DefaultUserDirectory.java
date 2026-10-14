package com.m000gg.billing.plugin.context;

import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserRepository;
import com.m000gg.plugin.api.query.user.UserContact;
import com.m000gg.plugin.api.query.user.UserDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DefaultUserDirectory implements UserDirectory {

    private final ApplicationUserRepository applicationUserRepository;

    @Override
    public Optional<UserContact> findContact(UUID userId) {
        return applicationUserRepository.findById(userId)
                .map(this::createUserContactFromApplicationUser);
    }

    private UserContact createUserContactFromApplicationUser(ApplicationUser applicationUser) {
        return new UserContact(applicationUser.getEmail());
    }
}
