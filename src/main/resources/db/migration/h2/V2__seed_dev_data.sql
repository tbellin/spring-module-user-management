-- V2__seed_dev_data.sql
-- Dev-only seed data: test user with ADMIN role
-- Username: tiziano, Email: tizianobellin@yahoo.com

-- Insert test user (password hash is BCrypt of 'password123' -- for dev use only)
INSERT INTO app_user (email, username, password_hash, first_name, last_name, enabled, email_verified)
VALUES (
    'tizianobellin@yahoo.com',
    'tiziano',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Tiziano',
    'Bellin',
    TRUE,
    TRUE
);

-- Assign ADMIN role to test user
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u, app_role r
WHERE u.username = 'tiziano' AND r.name = 'ROLE_ADMIN';
