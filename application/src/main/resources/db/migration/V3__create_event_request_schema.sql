-- V3__create_event_request_schema.sql
-- Adds event request persistence owned by mod-event

CREATE TABLE event_requests (
    id UUID PRIMARY KEY,
    tracking_code VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    requester_first_name VARCHAR(50) NOT NULL,
    requester_last_name VARCHAR(50) NOT NULL,
    requester_name VARCHAR(100) NOT NULL,
    requester_email VARCHAR(255) NOT NULL,
    requester_phone VARCHAR(20),
    requester_institution VARCHAR(255),
    event_name VARCHAR(200) NOT NULL,
    event_theme VARCHAR(500) NOT NULL,
    event_start_date DATE NOT NULL,
    event_end_date DATE NOT NULL,
    event_location VARCHAR(200) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    has_registration BOOLEAN NOT NULL,
    has_submission BOOLEAN NOT NULL,
    has_site BOOLEAN NOT NULL,
    admin_notes VARCHAR(1000),
    rejection_reason VARCHAR(1000),
    activation_token UUID,
    activation_token_expires_at TIMESTAMP,
    activation_token_used_at TIMESTAMP,
    linked_event_id UUID REFERENCES events(id),
    linked_user_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE event_request_status_history (
    id UUID PRIMARY KEY,
    event_request_id UUID NOT NULL REFERENCES event_requests(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    message VARCHAR(1000),
    actor_user_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_event_requests_status_created_at
    ON event_requests(status, created_at DESC);

CREATE INDEX idx_event_requests_requester_email
    ON event_requests(requester_email);

CREATE INDEX idx_event_requests_requester_event_status
    ON event_requests(requester_email, event_name, status);

CREATE INDEX idx_event_requests_activation_token
    ON event_requests(activation_token);

CREATE INDEX idx_event_request_history_request_id_created_at
    ON event_request_status_history(event_request_id, created_at);