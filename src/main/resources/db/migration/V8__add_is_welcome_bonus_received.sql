ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_welcome_bonus_received BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_users_welcome_bonus_pending
    ON users (is_welcome_bonus_received)
    WHERE is_welcome_bonus_received = FALSE;
