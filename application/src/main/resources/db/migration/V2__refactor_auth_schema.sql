-- V2__refactor_auth_schema.sql
-- Refactors the users table and adds auth-specific tables

-- 1. Add new columns to users
ALTER TABLE users ADD COLUMN first_name VARCHAR(50);
ALTER TABLE users ADD COLUMN last_name  VARCHAR(50);
ALTER TABLE users ADD COLUMN phone      VARCHAR(20);
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Migrate name → first_name + last_name (split on first space)
UPDATE users SET
  first_name = CASE WHEN POSITION(' ' IN name) > 0 THEN SUBSTRING(name, 1, POSITION(' ' IN name) - 1) ELSE name END,
  last_name  = CASE WHEN POSITION(' ' IN name) > 0 THEN SUBSTRING(name, POSITION(' ' IN name) + 1) ELSE '' END;

-- 3. Migrate password → password_hash (values already bcrypt-hashed)
UPDATE users SET password_hash = password;

-- 4. Make new columns NOT NULL after data migration
ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name  SET NOT NULL;
ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;

-- 5. Drop old columns
ALTER TABLE users DROP COLUMN name;
ALTER TABLE users DROP COLUMN password;
ALTER TABLE users DROP COLUMN role;
ALTER TABLE users DROP COLUMN active;

-- 6. Create user_roles join table (multi-role support)
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role)
);

-- 7. Assign default role to existing users
INSERT INTO user_roles (user_id, role)
SELECT id, 'PARTICIPANT' FROM users;

-- 8. Email verification tokens
CREATE TABLE email_verification_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. Password reset tokens
CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. User sessions
CREATE TABLE user_sessions (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash     VARCHAR(255) NOT NULL UNIQUE,
    expires_at     TIMESTAMP NOT NULL,
    invalidated_at TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. Indexes
CREATE INDEX idx_email_verification_tokens_token    ON email_verification_tokens(token);
CREATE INDEX idx_email_verification_tokens_user_id  ON email_verification_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token        ON password_reset_tokens(token);
CREATE INDEX idx_user_sessions_token_hash           ON user_sessions(token_hash);
CREATE INDEX idx_user_sessions_user_id              ON user_sessions(user_id);
