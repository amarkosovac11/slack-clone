CREATE TABLE workspaces (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_workspaces_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
);

CREATE TABLE workspace_members (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_workspace_members_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_workspace_members_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_workspace_member
        UNIQUE (workspace_id, user_id),

    CONSTRAINT chk_workspace_member_role
        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

CREATE INDEX idx_workspace_members_user_id
    ON workspace_members(user_id);

CREATE INDEX idx_workspace_members_workspace_id
    ON workspace_members(workspace_id);