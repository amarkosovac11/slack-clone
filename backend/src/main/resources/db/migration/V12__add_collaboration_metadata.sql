CREATE TABLE channel_message_mentions (
    message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (message_id, user_id)
);
CREATE INDEX idx_channel_mentions_user ON channel_message_mentions(user_id, message_id);

CREATE TABLE conversation_message_mentions (
    message_id BIGINT NOT NULL REFERENCES conversation_messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (message_id, user_id)
);
CREATE INDEX idx_conversation_mentions_user ON conversation_message_mentions(user_id, message_id);

CREATE TABLE channel_pinned_messages (
    message_id BIGINT PRIMARY KEY REFERENCES messages(id) ON DELETE CASCADE,
    channel_id BIGINT NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    pinned_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    pinned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_channel_pin UNIQUE(channel_id, message_id)
);
CREATE INDEX idx_channel_pins_list ON channel_pinned_messages(channel_id, pinned_at DESC);
