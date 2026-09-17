CREATE TABLE conversation_message_deliveries (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES conversation_messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    delivered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_conversation_delivery_message_user UNIQUE (message_id, user_id)
);

CREATE INDEX idx_conversation_deliveries_user_message
    ON conversation_message_deliveries(user_id, message_id);
