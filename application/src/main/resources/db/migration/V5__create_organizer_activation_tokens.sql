-- V5__create_organizer_activation_tokens.sql
-- Adds organizer activation tokens owned by mod-auth
-- Generated for feature: 002-organizer-activation-registration

CREATE TABLE organizer_activation_tokens (
    id                UUID         PRIMARY KEY,
    token             VARCHAR(36)  NOT NULL,
    first_name        VARCHAR(50)  NOT NULL,
    last_name         VARCHAR(50)  NOT NULL,
    email             VARCHAR(255) NOT NULL,
    phone             VARCHAR(20),
    event_name        VARCHAR(255) NOT NULL,
    event_id          UUID         NOT NULL,
    event_request_id  UUID         NOT NULL,
    expires_at        TIMESTAMP    NOT NULL,
    used_at           TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    CONSTRAINT uq_organizer_activation_tokens_token UNIQUE (token)
);

CREATE INDEX idx_organizer_activation_tokens_email
    ON organizer_activation_tokens(email);

CREATE INDEX idx_organizer_activation_tokens_event_request_id
    ON organizer_activation_tokens(event_request_id);
