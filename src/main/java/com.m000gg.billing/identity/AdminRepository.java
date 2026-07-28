package com.m000gg.billing.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
    public Admin findByEmail (String email);
}
