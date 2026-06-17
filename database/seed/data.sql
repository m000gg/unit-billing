SET session_replication_role = 'replica';
TRUNCATE TABLE  application_users
RESTART IDENTITY CASCADE;

-- ==========================================================
-- 1. APPLICATION USERS
-- ==========================================================
INSERT INTO application_users (first_name, last_name, email, phone, password, balance, country, city, region, street, house_number, apartment, postal_code, is_deleted)
VALUES
    ('Ivan', 'Petrov', 'ivan@test.com', '+380991234567', '$2a$12$dR3OKM0Tqq8aj37/TwHx9e1BPVFxhcv/tnmAqBzcTBD5XrpZaguRS', 100.00, 'Ukraine', 'Kyiv', 'Kyiv', 'Khreshchatyk', '1', '12', '01001', false),
    ('Anna', 'Koval', 'anna@test.com', '+380997654321', '$2a$12$dR3OKM0Tqq8aj37/TwHx9e1BPVFxhcv/tnmAqBzcTBD5XrpZaguRS', 250.50, 'Ukraine', 'Lviv', 'Lviv', 'Svobody', '5', '3', '79000', false);

