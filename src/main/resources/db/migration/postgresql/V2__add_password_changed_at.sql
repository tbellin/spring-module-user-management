-- V2__add_password_changed_at.sql
-- Add password_changed_at column for JWT invalidation after password change

ALTER TABLE app_user ADD COLUMN password_changed_at TIMESTAMP;
