package com.amar.slackclone.workspace.dto;

import com.amar.slackclone.workspace.WorkspaceRole;

import java.time.Instant;

public record WorkspaceMemberResponse(
        Long userId,
        String displayName,
        String email,
        WorkspaceRole role,
        Instant joinedAt
) {
}
