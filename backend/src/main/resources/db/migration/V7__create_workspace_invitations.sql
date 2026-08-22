CREATE TABLE workspace_invitations (
    id BIGSERIAL PRIMARY KEY,

    workspace_id BIGINT NOT NULL,
    invited_user_id BIGINT NOT NULL,
    invited_by BIGINT NOT NULL,

    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_workspace_invitations_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_workspace_invitations_invited_user
        FOREIGN KEY (invited_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_workspace_invitations_invited_by
        FOREIGN KEY (invited_by)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_workspace_invitations_role
        CHECK (role IN ('ADMIN', 'MEMBER')),

    CONSTRAINT chk_workspace_invitations_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uq_workspace_invitations_pending
    ON workspace_invitations(workspace_id, invited_user_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_workspace_invitations_invited_user
    ON workspace_invitations(invited_user_id);

CREATE INDEX idx_workspace_invitations_workspace
    ON workspace_invitations(workspace_id);