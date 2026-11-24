--liquibase formatted sql

--changeset user-service:001-create-users

CREATE TABLE users (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

                       name VARCHAR(100) NOT NULL,
                       surname VARCHAR(100) NOT NULL,
                       birth_date DATE,
                       email VARCHAR(320) NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE,

                       created_at TIMESTAMPTZ NOT NULL,
                       updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_users_email
    ON users(email);