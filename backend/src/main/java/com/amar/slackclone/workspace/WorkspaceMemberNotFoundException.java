package com.amar.slackclone.workspace;

public class WorkspaceMemberNotFoundException extends RuntimeException {
    public WorkspaceMemberNotFoundException(Long workspaceId, Long userId) {
        super("User " + userId + " is not a member of workspace " + workspaceId);
    }
}
