ALTER TABLE channels ADD COLUMN archived_at TIMESTAMP WITH TIME ZONE NULL;

CREATE INDEX idx_channels_workspace_archived
    ON channels(workspace_id, archived_at);
