package com.amar.slackclone.workspace.dto; import jakarta.validation.constraints.NotNull;
public record TransferWorkspaceOwnershipRequest(@NotNull Long newOwnerUserId){}
