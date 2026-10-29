--liquibase formatted sql

--changeset user-service:003-add-unique-email

UPDATE users
SET email = LOWER(BTRIM(email));

ALTER TABLE users
    ADD CONSTRAINT uq_users_email UNIQUE (email);

DROP INDEX IF EXISTS idx_users_email;
