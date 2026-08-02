package com.amar.slackclone.workspace.dto;

import com.amar.slackclone.workspace.WorkspaceRole;

import java.time.Instant;

public record WorkspaceResponse(
        Long id,
        String name,
        String slug,
        Long ownerId,
        WorkspaceRole currentUserRole,
        Instant createdAt,
        Instant updatedAt
) {
}