-- V6__drop_organizer_activation_tokens.sql
-- Removes the organizer_activation_tokens table. All activation token data is now
-- stored exclusively in event_requests (activation_token, activation_token_expires_at,
-- activation_token_used_at). mod-auth activation operations delegate to mod-event.

DROP INDEX IF EXISTS idx_organizer_activation_tokens_event_request_id;
DROP INDEX IF EXISTS idx_organizer_activation_tokens_email;
DROP TABLE IF EXISTS organizer_activation_tokens;
