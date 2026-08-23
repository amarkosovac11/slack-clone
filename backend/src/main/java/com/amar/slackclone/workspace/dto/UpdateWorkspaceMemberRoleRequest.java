package com.amar.slackclone.workspace.dto;

import com.amar.slackclone.workspace.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkspaceMemberRoleRequest(
        @NotNull WorkspaceRole role
) {
}
