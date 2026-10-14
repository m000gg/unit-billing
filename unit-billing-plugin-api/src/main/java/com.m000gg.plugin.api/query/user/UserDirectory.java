package com.m000gg.plugin.api.query.user;

import java.util.Optional;
import java.util.UUID;

public interface UserDirectory {

    /**
     * Looks up a user info by its id.
     *
     * @return the user info, or empty if there is no such user
     */
    Optional<UserContact> findContact(UUID userId);

}
