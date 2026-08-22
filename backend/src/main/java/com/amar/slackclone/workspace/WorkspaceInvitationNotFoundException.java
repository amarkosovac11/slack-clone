package com.amar.slackclone.workspace;

public class WorkspaceInvitationNotFoundException extends RuntimeException {

    public WorkspaceInvitationNotFoundException(Long invitationId) {
        super("Workspace invitation not found: " + invitationId);
    }
}
