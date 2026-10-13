/*
 * Copyright 2026 Vladyslav Livandovskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m000gg.billing.settings;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_settings")
public class SystemSetting {
    @Id
    @Column(name = "setting_key", length = 50)
    private String key;

    @Column(name = "setting_value")
    private String settingValue;

    private String description;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getSettingValue() {
        return settingValue;
    }
    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
