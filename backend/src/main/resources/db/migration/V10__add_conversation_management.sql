ALTER TABLE conversations
    ADD COLUMN custom_name VARCHAR(100),
    ADD CONSTRAINT chk_group_custom_name CHECK (custom_name IS NULL OR type = 'GROUP');

ALTER TABLE conversation_participants
    ADD COLUMN hidden_at TIMESTAMPTZ;

ALTER TABLE conversation_messages
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE conversation_messages
    DROP CONSTRAINT IF EXISTS conversation_messages_content_check;

ALTER TABLE conversation_messages
    ALTER COLUMN content DROP NOT NULL;

ALTER TABLE conversation_messages
    ADD CONSTRAINT chk_conversation_message_content CHECK (
        (deleted_at IS NULL AND content IS NOT NULL AND length(trim(content)) > 0)
        OR (deleted_at IS NOT NULL AND content IS NULL)
    );

CREATE INDEX idx_conversation_participants_visible
    ON conversation_participants(user_id, conversation_id) WHERE hidden_at IS NULL;
