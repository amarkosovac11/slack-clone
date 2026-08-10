CREATE TABLE channel_members (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_channel_members_channel
        FOREIGN KEY (channel_id)
        REFERENCES channels(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_channel_members_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_channel_member
        UNIQUE (channel_id, user_id)
);

CREATE INDEX idx_channel_members_channel_id
ON channel_members(channel_id);

CREATE INDEX idx_channel_members_user_id
ON channel_members(user_id);