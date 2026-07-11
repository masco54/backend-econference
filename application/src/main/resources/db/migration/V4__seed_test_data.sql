-- V4__seed_test_data.sql
-- Seed data for testing: creates a user and an event request

-- 3. Insert event request for the test user
INSERT INTO event_requests (
    id,
    tracking_code,
    status,
    requester_first_name,
    requester_last_name,
    requester_name,
    requester_email,
    requester_phone,
    requester_institution,
    event_name,
    event_theme,
    event_start_date,
    event_end_date,
    event_location,
    event_type,
    has_registration,
    has_submission,
    has_site,
    admin_notes,
    rejection_reason,
    activation_token,
    activation_token_expires_at,
    activation_token_used_at,
    linked_event_id,
    linked_user_id,
    created_at,
    updated_at
) VALUES (
    '650e8400-e29b-41d4-a716-446655440001'::uuid,
    'SOL-2026-0001',
    'REQUESTED',
    'Marco',
    'Silva Costa',
    'Marco Silva Costa',
    'marcosilvacosta54@gmail.com',
    NULL,
    'UNIFESP',
    'Conference on Technology and Innovation',
    'Exploring the future of technology and its impact on society',
    '2026-08-15',
    '2026-08-17',
    'São Paulo, Brazil',
    'CONFERENCE',
    true,
    true,
    true,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 4. Insert initial status history entry
INSERT INTO event_request_status_history (
    id,
    event_request_id,
    status,
    message,
    actor_user_id,
    created_at,
    updated_at
) VALUES (
    '750e8400-e29b-41d4-a716-446655440001'::uuid,
    '650e8400-e29b-41d4-a716-446655440001'::uuid,
    'REQUESTED',
    'Solicitação de evento submetida',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

