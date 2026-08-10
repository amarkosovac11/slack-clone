package com.amar.slackclone.channel;

public class WorkspaceNotFoundException extends RuntimeException {

    public WorkspaceNotFoundException(Long workspaceId) {
        super("Workspace with id " + workspaceId + " was not found");
    }
}