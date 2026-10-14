package com.m000gg.plugin.api.query.user;

import java.util.Optional;
import java.util.UUID;

public interface UserDirectory {

    Optional<UserContact> findContact(UUID userId);

}
