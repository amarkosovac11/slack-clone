ALTER TABLE conversation_participants ADD COLUMN left_at TIMESTAMPTZ;

CREATE INDEX idx_conversation_participants_active
    ON conversation_participants(conversation_id, user_id)
    WHERE left_at IS NULL;

DROP INDEX IF EXISTS idx_conversation_participants_visible;
CREATE INDEX idx_conversation_participants_visible
    ON conversation_participants(user_id, conversation_id)
    WHERE hidden_at IS NULL AND left_at IS NULL;
