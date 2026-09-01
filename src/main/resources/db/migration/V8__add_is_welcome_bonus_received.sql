-- Safe add for existing rows: nullable first, backfill, then NOT NULL + default.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_welcome_bonus_received BOOLEAN;

UPDATE users
SET is_welcome_bonus_received = FALSE
WHERE is_welcome_bonus_received IS NULL;

ALTER TABLE users
    ALTER COLUMN is_welcome_bonus_received SET DEFAULT FALSE;

ALTER TABLE users
    ALTER COLUMN is_welcome_bonus_received SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_welcome_bonus_pending
    ON users (is_welcome_bonus_received)
    WHERE is_welcome_bonus_received = FALSE;
