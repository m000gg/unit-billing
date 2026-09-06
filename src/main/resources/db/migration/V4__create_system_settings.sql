CREATE TABLE system_settings (
                                 setting_key VARCHAR(50) PRIMARY KEY,
                                 setting_value TEXT,
                                 description VARCHAR(255),
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);