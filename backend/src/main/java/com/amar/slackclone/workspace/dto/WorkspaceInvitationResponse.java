package com.amar.slackclone.workspace.dto;

import com.amar.slackclone.workspace.WorkspaceInvitationStatus;
import com.amar.slackclone.workspace.WorkspaceRole;

import java.time.Instant;

public record WorkspaceInvitationResponse(
        Long id,

        Long workspaceId,
        String workspaceName,

        Long invitedUserId,
        String invitedUserDisplayName,
        String invitedUserEmail,

        Long invitedByUserId,
        String invitedByDisplayName,

        WorkspaceRole role,
        WorkspaceInvitationStatus status,

        Instant createdAt,
        Instant expiresAt,
        Instant acceptedAt
) {
}