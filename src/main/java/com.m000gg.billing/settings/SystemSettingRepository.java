package com.m000gg.billing.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
    @Query("SELECT s.settingValue FROM SystemSetting s WHERE s.key = :key")
    Optional<String> findValueByKey(@Param("key") String key);
}
