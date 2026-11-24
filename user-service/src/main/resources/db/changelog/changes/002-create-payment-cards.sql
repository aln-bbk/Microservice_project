--liquibase formatted sql

--changeset user-service:002-create-payment-cards

CREATE TABLE payment_cards (
                               id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

                               user_id BIGINT NOT NULL,

                               number VARCHAR(19) NOT NULL,
                               holder VARCHAR(200) NOT NULL,
                               expiration_date DATE NOT NULL,

                               active BOOLEAN NOT NULL DEFAULT TRUE,

                               created_at TIMESTAMPTZ NOT NULL,
                               updated_at TIMESTAMPTZ NOT NULL,

                               CONSTRAINT fk_payment_cards_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
);

CREATE INDEX idx_payment_cards_user_id
    ON payment_cards(user_id);