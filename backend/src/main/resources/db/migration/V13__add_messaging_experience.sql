ALTER TABLE messages ADD COLUMN thread_root_message_id BIGINT REFERENCES messages(id) ON DELETE CASCADE;
ALTER TABLE conversation_messages ADD COLUMN thread_root_message_id BIGINT REFERENCES conversation_messages(id) ON DELETE CASCADE;
CREATE INDEX idx_messages_thread ON messages(thread_root_message_id, created_at);
CREATE INDEX idx_conversation_messages_thread ON conversation_messages(thread_root_message_id, created_at);

CREATE TABLE channel_message_reactions(id BIGSERIAL PRIMARY KEY,message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,emoji VARCHAR(16) NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),UNIQUE(message_id,user_id,emoji));
CREATE INDEX idx_channel_reactions_message ON channel_message_reactions(message_id);
CREATE TABLE conversation_message_reactions(id BIGSERIAL PRIMARY KEY,message_id BIGINT NOT NULL REFERENCES conversation_messages(id) ON DELETE CASCADE,user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,emoji VARCHAR(16) NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),UNIQUE(message_id,user_id,emoji));
CREATE INDEX idx_conversation_reactions_message ON conversation_message_reactions(message_id);

CREATE TABLE channel_message_attachments(id BIGSERIAL PRIMARY KEY,message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,uploaded_by BIGINT NOT NULL REFERENCES users(id),original_file_name VARCHAR(255) NOT NULL,storage_key VARCHAR(100) NOT NULL UNIQUE,mime_type VARCHAR(100) NOT NULL,file_size BIGINT NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE INDEX idx_channel_attachments_message ON channel_message_attachments(message_id);
CREATE TABLE conversation_message_attachments(id BIGSERIAL PRIMARY KEY,message_id BIGINT NOT NULL REFERENCES conversation_messages(id) ON DELETE CASCADE,uploaded_by BIGINT NOT NULL REFERENCES users(id),original_file_name VARCHAR(255) NOT NULL,storage_key VARCHAR(100) NOT NULL UNIQUE,mime_type VARCHAR(100) NOT NULL,file_size BIGINT NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE INDEX idx_conversation_attachments_message ON conversation_message_attachments(message_id);
