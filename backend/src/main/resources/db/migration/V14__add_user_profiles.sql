ALTER TABLE users
    ADD COLUMN title VARCHAR(120),
    ADD COLUMN avatar_key VARCHAR(100),
    ADD COLUMN custom_status_text VARCHAR(100),
    ADD COLUMN custom_status_emoji VARCHAR(32),
    ADD COLUMN custom_status_expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_seen_at TIMESTAMP WITH TIME ZONE;
