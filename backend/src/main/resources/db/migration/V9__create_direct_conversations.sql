CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    direct_key VARCHAR(80) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_conversation_direct_key CHECK (
        (type = 'DIRECT' AND direct_key IS NOT NULL) OR
        (type = 'GROUP' AND direct_key IS NULL)
    )
);

CREATE TABLE conversation_participants (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_read_message_id BIGINT,
    CONSTRAINT uq_conversation_participant UNIQUE (conversation_id, user_id)
);

CREATE TABLE conversation_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL CHECK (length(trim(content)) > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE conversation_participants ADD CONSTRAINT fk_participant_last_read_message
    FOREIGN KEY (last_read_message_id) REFERENCES conversation_messages(id) ON DELETE SET NULL;

CREATE INDEX idx_conversation_participants_user ON conversation_participants(user_id, conversation_id);
CREATE INDEX idx_conversation_messages_history ON conversation_messages(conversation_id, id DESC);
CREATE INDEX idx_conversations_activity ON conversations(updated_at DESC);
